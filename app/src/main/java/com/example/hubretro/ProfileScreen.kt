package com.example.hubretro

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
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

fun gameAccentColor(gameName: String): Color {
    val n = gameName.lowercase()
    return when {
        n.contains("final fantasy") || n.contains("zelda") || n.contains("chrono") ||
                n.contains("persona") || n.contains("dragon") || n.contains("earthbound") ||
                n.contains("undertale") || n.contains("pokemon") -> Color(0xFF8E24AA)
        n.contains("sonic") || n.contains("infamous") || n.contains("god of war") ||
                n.contains("devil may cry") || n.contains("cyberpunk") || n.contains("batman") -> Color(0xFFE53935)
        n.contains("mario") || n.contains("fez") || n.contains("kirby") ||
                n.contains("crash") || n.contains("hollow knight") || n.contains("celeste") -> Color(0xFF1565C0)
        n.contains("doom") || n.contains("halo") || n.contains("half-life") ||
                n.contains("metroid") || n.contains("quake") -> Color(0xFF00838F)
        n.contains("minecraft") || n.contains("uncharted") || n.contains("tomb raider") ||
                n.contains("shadow") || n.contains("ico") -> Color(0xFF2E7D32)
        n.contains("pac-man") || n.contains("tetris") || n.contains("street fighter") ||
                n.contains("sims") || n.contains("mega man") || n.contains("galaga") -> Color(0xFFEF6C00)
        else -> Color(0xFFFFB300)
    }
}

fun detectGenreForGame(gameName: String): String {
    val n = gameName.lowercase()
    return when {
        n.contains("final fantasy") || n.contains("zelda") || n.contains("chrono") ||
                n.contains("persona") || n.contains("dragon") || n.contains("earthbound") ||
                n.contains("undertale") || n.contains("pokemon") -> "RPG"
        n.contains("sonic") || n.contains("infamous") || n.contains("god of war") ||
                n.contains("devil may cry") || n.contains("cyberpunk") || n.contains("batman") -> "ACTION"
        n.contains("mario") || n.contains("fez") || n.contains("kirby") ||
                n.contains("crash") || n.contains("hollow knight") || n.contains("celeste") -> "PLATFORMER"
        n.contains("doom") || n.contains("halo") || n.contains("half-life") ||
                n.contains("metroid") || n.contains("quake") -> "SHOOTER"
        n.contains("minecraft") || n.contains("uncharted") || n.contains("tomb raider") ||
                n.contains("shadow") || n.contains("ico") -> "ADVENTURE"
        n.contains("pac-man") || n.contains("tetris") || n.contains("street fighter") ||
                n.contains("sims") || n.contains("mega man") || n.contains("galaga") -> "ARCADE"
        else -> "GAMING"
    }
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
    var editHabboRegion by remember { mutableStateOf("habbo.com") }
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
            editYoutube = it.youtubeUsername; editHabbo = it.habboUsername
            editHabboRegion = it.habboRegion.ifBlank { "habbo.com" }
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
    val displayHabbo = firebaseProfile?.habboUsername ?: ""
    val displayHabboRegion = firebaseProfile?.habboRegion?.ifBlank { "habbo.com" } ?: "habbo.com"

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

            // ─── Header ───────────────────────────────────────────────────────
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
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(ScrapbookDark).border(2.dp, ScrapbookBorder, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
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
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                        item { ScrapbookStatCard(formatCount(displayFollowersCount), "FOLLOWERS", neonAlpha) { showFollowersList = true } }
                        item { ScrapbookStatCard(formatCount(displayFollowingCount), "FOLLOWING", neonAlpha) { showFollowingList = true } }
                        item { ScrapbookStatCard("${achievementsState.articleCount}", "ARTICLES", neonAlpha) { selectedTab = 0 } }
                        item { ScrapbookStatCard("${achievementsState.bookmarkCount}", "BOOKMARKS", neonAlpha) { selectedTab = 1 } }
                        item { ScrapbookStatCard("${achievementsState.xp}", "TOTAL XP", neonAlpha) { } }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.horizontalGradient(colors = listOf(ScrapbookDark, Color(0xFF2A2A4A))))
                                .border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = RoundedCornerShape(10.dp))
                                .clickable { isEditing = true }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Edit, contentDescription = null, tint = ScrapbookYellow, modifier = Modifier.size(16.dp))
                                Text("EDIT PROFILE", fontFamily = BangersFontFamily, fontSize = 18.sp, color = ScrapbookYellow, letterSpacing = 1.sp)
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    // ─── Edit Mode ────────────────────────────────────────────
                    if (isEditing) {
                        item {
                            var expandedSections by remember { mutableStateOf(setOf("BASIC")) }
                            var hasUnsavedChanges by remember { mutableStateOf(false) }
                            var showUnsavedDialog by remember { mutableStateOf(false) }

                            LaunchedEffect(editUsername, editBio, editHandle, editLocation, editWebsite, editPsn, editXbox, editSteam, editNintendo, editTwitch, editYoutube, editHabbo, editHabboRegion) {
                                hasUnsavedChanges = true
                            }

                            val completionFields = listOf(
                                editUsername.isNotBlank(), editBio.isNotBlank(), editHandle.isNotBlank(),
                                editLocation.isNotBlank(), editWebsite.isNotBlank(),
                                editPsn.isNotBlank() || editXbox.isNotBlank() || editSteam.isNotBlank() || editNintendo.isNotBlank(),
                                editTwitch.isNotBlank() || editYoutube.isNotBlank(),
                                editHabbo.isNotBlank()
                            )
                            val completionPct = (completionFields.count { it } * 100f / completionFields.size).toInt()

                            val editNeonT = rememberInfiniteTransition(label = "editNeon")
                            val editNeonAlpha by editNeonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "editNeonAlpha")

                            if (showUnsavedDialog) {
                                AlertDialog(
                                    onDismissRequest = { showUnsavedDialog = false },
                                    containerColor = ScrapbookDark,
                                    titleContentColor = ScrapbookYellow,
                                    textContentColor = Color.White,
                                    title = { Text("⚠️ UNSAVED CHANGES", fontFamily = BangersFontFamily, fontSize = 20.sp) },
                                    text = { Text("You have unsaved changes. Save before leaving?", fontFamily = NunitoFontFamily, fontSize = 14.sp) },
                                    confirmButton = {
                                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookYellow).clickable {
                                            showUnsavedDialog = false
                                            firebaseProfile?.let { authViewModel.updateUserProfile(it.copy(username = editUsername, bio = editBio, userHandle = editHandle, location = editLocation, website = editWebsite, psnUsername = editPsn, xboxUsername = editXbox, steamUsername = editSteam, nintendoUsername = editNintendo, twitchUsername = editTwitch, youtubeUsername = editYoutube, habboUsername = editHabbo)) }
                                            if (editHabbo.isNotBlank()) authViewModel.updateHabboUsername(editHabbo, editHabboRegion)
                                            isEditing = false; achievementsViewModel.fetchAchievements()
                                        }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                            Text("SAVE & EXIT", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                                        }
                                    },
                                    dismissButton = {
                                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookRed.copy(alpha = 0.15f)).border(1.dp, ScrapbookRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).clickable { showUnsavedDialog = false; isEditing = false }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                            Text("DISCARD", fontFamily = BangersFontFamily, color = ScrapbookRed, fontSize = 14.sp)
                                        }
                                    }
                                )
                            }

                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                Spacer(modifier = Modifier.height(8.dp))

                                // Completion bar
                                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ScrapbookDark).border(1.dp, ScrapbookYellow.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(14.dp)) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("PROFILE COMPLETION", fontFamily = BangersFontFamily, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, letterSpacing = 1.sp)
                                            Text("$completionPct%", fontFamily = BangersFontFamily, color = when { completionPct >= 80 -> Color(0xFF4CAF50); completionPct >= 50 -> ScrapbookYellow; else -> ScrapbookRed }, fontSize = 18.sp)
                                        }
                                        val animatedCompletion by animateFloatAsState(targetValue = completionPct / 100f, animationSpec = tween(800, easing = LinearOutSlowInEasing), label = "completionAnim")
                                        Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color.White.copy(alpha = 0.08f))) {
                                            Box(modifier = Modifier.fillMaxWidth(animatedCompletion).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(Brush.horizontalGradient(colors = when { completionPct >= 80 -> listOf(Color(0xFF4CAF50), Color(0xFF81C784)); completionPct >= 50 -> listOf(ScrapbookYellow, ScrapbookYellowDark); else -> listOf(ScrapbookRed, ScrapbookRed.copy(alpha = 0.7f)) })))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Unsaved indicator
                                AnimatedVisibility(visible = hasUnsavedChanges, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(ScrapbookYellow.copy(alpha = 0.08f)).border(1.dp, ScrapbookYellow.copy(alpha = editNeonAlpha * 0.6f), RoundedCornerShape(8.dp)).padding(horizontal = 14.dp, vertical = 8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(ScrapbookYellow.copy(alpha = editNeonAlpha)))
                                            Text("CHANGES UNSAVED", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 12.sp, letterSpacing = 1.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Quick jump chips
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                                    val sections = listOf<Pair<String, Color>>(
                                        "BASIC" to ScrapbookYellow,
                                        "PLATFORMS" to Color(0xFF1565C0),
                                        "STREAMING" to Color(0xFF6A1B9A),
                                        "HABBO" to Color(0xFF00695C)
                                    )
                                    items(sections) { (key, color) ->
                                        val isActive = expandedSections.contains(key)
                                        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (isActive) color.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)).border(1.5.dp, if (isActive) color.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp)).clickable { expandedSections = if (isActive) expandedSections - key else expandedSections + key }.padding(horizontal = 14.dp, vertical = 7.dp)) {
                                            Text(key, fontFamily = BangersFontFamily, color = if (isActive) color else Color.White.copy(alpha = 0.4f), fontSize = 11.sp, letterSpacing = 1.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // BASIC INFO
                                val basicFilled = listOf(editUsername.isNotBlank(), editHandle.isNotBlank(), editBio.isNotBlank(), editLocation.isNotBlank(), editWebsite.isNotBlank())
                                val basicFraction = basicFilled.count { !it }.toFloat() / basicFilled.size
                                AccordionSection(emoji = "👤", title = "BASIC INFO", accentColor = ScrapbookYellow, isExpanded = expandedSections.contains("BASIC"), completionFraction = basicFraction, onToggle = { expandedSections = if (expandedSections.contains("BASIC")) expandedSections - "BASIC" else expandedSections + "BASIC" }) {
                                    RetroTerminalInput(value = editUsername, onValueChange = { editUsername = it }, label = "USERNAME", maxChars = 20)
                                    RetroTerminalInput(value = editHandle, onValueChange = { v -> editHandle = if (v.isNotBlank() && !v.startsWith("@")) "@$v" else v }, label = "HANDLE", maxChars = 20)
                                    RetroTerminalInput(value = editBio, onValueChange = { editBio = it }, label = "BIO", maxChars = 150, singleLine = false)
                                    RetroTerminalInput(value = editLocation, onValueChange = { editLocation = it }, label = "LOCATION")
                                    RetroTerminalInput(value = editWebsite, onValueChange = { v -> editWebsite = if (v.isNotBlank() && !v.startsWith("http") && v.contains(".")) "https://$v" else v }, label = "WEBSITE")
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // GAMING PLATFORMS
                                val platformFilled = listOf(editPsn.isNotBlank(), editXbox.isNotBlank(), editSteam.isNotBlank(), editNintendo.isNotBlank())
                                val platformFraction = platformFilled.count { !it }.toFloat() / platformFilled.size
                                AccordionSection(emoji = "🎮", title = "GAMING PLATFORMS", accentColor = Color(0xFF1565C0), isExpanded = expandedSections.contains("PLATFORMS"), completionFraction = platformFraction, onToggle = { expandedSections = if (expandedSections.contains("PLATFORMS")) expandedSections - "PLATFORMS" else expandedSections + "PLATFORMS" }) {
                                    PlatformInputWithVerify(editPsn, { editPsn = it }, gamingPlatforms[0])
                                    PlatformInputWithVerify(editXbox, { editXbox = it }, gamingPlatforms[1])
                                    PlatformInputWithVerify(editSteam, { editSteam = it }, gamingPlatforms[2])
                                    PlatformInputWithVerify(editNintendo, { editNintendo = it }, gamingPlatforms[3])
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // STREAMING
                                val streamFilled = listOf(editTwitch.isNotBlank(), editYoutube.isNotBlank())
                                val streamFraction = streamFilled.count { !it }.toFloat() / streamFilled.size
                                AccordionSection(emoji = "📡", title = "STREAMING", accentColor = Color(0xFF6A1B9A), isExpanded = expandedSections.contains("STREAMING"), completionFraction = streamFraction, onToggle = { expandedSections = if (expandedSections.contains("STREAMING")) expandedSections - "STREAMING" else expandedSections + "STREAMING" }) {
                                    RetroTerminalInput(value = editTwitch, onValueChange = { editTwitch = it }, label = "TWITCH USERNAME")
                                    RetroTerminalInput(value = editYoutube, onValueChange = { editYoutube = it }, label = "YOUTUBE USERNAME")
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // HABBO HOTEL
                                val habboFraction = if (editHabbo.isBlank()) 1f else 0f
                                AccordionSection(emoji = "🏨", title = "HABBO HOTEL", accentColor = Color(0xFF00695C), isExpanded = expandedSections.contains("HABBO"), completionFraction = habboFraction, onToggle = { expandedSections = if (expandedSections.contains("HABBO")) expandedSections - "HABBO" else expandedSections + "HABBO" }) {
                                    Text("▶ SELECT YOUR HOTEL", fontFamily = BangersFontFamily, color = ScrapbookYellow.copy(alpha = 0.7f), fontSize = 11.sp, letterSpacing = 1.sp)
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)
                                    ) {
                                        items(habboRegions) { (label, domain, _) ->
                                            val isSelected = editHabboRegion == domain
                                            var regionPressed by remember { mutableStateOf(false) }
                                            val regionScale by animateFloatAsState(targetValue = if (regionPressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "regionScale")
                                            Box(modifier = Modifier.scale(regionScale).clip(RoundedCornerShape(20.dp)).background(if (isSelected) Color(0xFF00695C).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f)).border(width = if (isSelected) 2.dp else 1.dp, color = if (isSelected) Color(0xFF4DB6AC) else Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp)).clickable { regionPressed = true; editHabboRegion = domain }.padding(horizontal = 12.dp, vertical = 7.dp)) {
                                                Text(text = label, fontFamily = BangersFontFamily, color = if (isSelected) Color(0xFF4DB6AC) else Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                            }
                                            LaunchedEffect(regionPressed) { if (regionPressed) { delay(150); regionPressed = false } }
                                        }
                                    }
                                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF00695C).copy(alpha = 0.1f)).border(1.dp, Color(0xFF4DB6AC).copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("🌐", fontSize = 14.sp)
                                            Text("Hotel: www.$editHabboRegion", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                                        }
                                    }
                                    RetroTerminalInput(value = editHabbo, onValueChange = { editHabbo = it }, label = "HABBO USERNAME")
                                    if (editHabbo.isNotBlank()) {
                                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF0D0D1A)).border(1.5.dp, Color(0xFF4DB6AC).copy(alpha = 0.5f), RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                            AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(habboAvatarUrl(editHabbo, editHabboRegion)).crossfade(true).diskCachePolicy(CachePolicy.DISABLED).memoryCachePolicy(CachePolicy.DISABLED).build(), contentDescription = "Habbo Avatar Preview", contentScale = ContentScale.Fit, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.3f)), error = rememberVectorPainter(image = Icons.Filled.SmartToy), fallback = rememberVectorPainter(image = Icons.Filled.SmartToy))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF4DB6AC).copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 3.dp)) { Text("LIVE PREVIEW", fontFamily = BangersFontFamily, color = Color(0xFF4DB6AC), fontSize = 10.sp) }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(editHabbo, fontFamily = BangersFontFamily, color = Color.White, fontSize = 16.sp)
                                                Text("www.$editHabboRegion", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                                                Text("🤖 This will be your talking robot", fontFamily = NunitoFontFamily, color = Color(0xFF4DB6AC).copy(alpha = 0.7f), fontSize = 10.sp)
                                            }
                                        }
                                    } else {
                                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.03f)).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp)).padding(16.dp), contentAlignment = Alignment.Center) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Text("🏨", fontSize = 28.sp)
                                                Column {
                                                    Text("ADD YOUR HABBO USERNAME", fontFamily = BangersFontFamily, color = Color(0xFF4DB6AC), fontSize = 14.sp)
                                                    Text("Pick your hotel above first!", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Save button
                                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ScrapbookDark).border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = editNeonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = editNeonAlpha))), shape = RoundedCornerShape(12.dp)).clickable {
                                    firebaseProfile?.let { authViewModel.updateUserProfile(it.copy(username = editUsername, bio = editBio, userHandle = editHandle, location = editLocation, website = editWebsite, psnUsername = editPsn, xboxUsername = editXbox, steamUsername = editSteam, nintendoUsername = editNintendo, twitchUsername = editTwitch, youtubeUsername = editYoutube, habboUsername = editHabbo)) }
                                    if (editHabbo.isNotBlank()) authViewModel.updateHabboUsername(editHabbo, editHabboRegion)
                                    currentUser?.uid?.let { uid -> com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid).set(mapOf("profileCompletion" to completionPct), com.google.firebase.firestore.SetOptions.merge()) }
                                    hasUnsavedChanges = false; isEditing = false; achievementsViewModel.fetchAchievements()
                                }.padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = ScrapbookYellow, modifier = Modifier.size(20.dp))
                                        Text("SAVE PROFILE", fontFamily = BangersFontFamily, fontSize = 20.sp, color = ScrapbookYellow, letterSpacing = 1.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Cancel button
                                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.Transparent).border(1.dp, ScrapbookRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).clickable { if (hasUnsavedChanges) showUnsavedDialog = true else isEditing = false }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                                    Text("CANCEL", fontFamily = BangersFontFamily, fontSize = 16.sp, color = ScrapbookRed.copy(alpha = 0.6f), letterSpacing = 1.sp)
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }

                    // ─── View Mode ────────────────────────────────────────────
                    if (!isEditing) {
                        item {
                            ScrapbookSectionHeader("ABOUT ME", "👤")
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                BioCard(bioText = displayBio, profilePicUrl = displayProfilePicUrl, habboUsername = displayHabbo, habboRegion = displayHabboRegion)
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

                                // Habbo Hotel Card
                                if (displayHabbo.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val habboNeonT = rememberInfiniteTransition(label = "habboNeon")
                                    val habboNeonAlpha by habboNeonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse), label = "habboNeonAlpha")
                                    val flickerT = rememberInfiniteTransition(label = "habboFlicker")
                                    val flickerAlpha by flickerT.animateFloat(initialValue = 0.85f, targetValue = 1f, animationSpec = infiniteRepeatable(keyframes { durationMillis = 3000; 1f at 0; 0.7f at 100; 1f at 200; 1f at 1400; 0.6f at 1500; 1f at 1600; 1f at 2800; 0.8f at 2850; 1f at 2900 }, RepeatMode.Restart), label = "flickerAlpha")
                                    var avatarVisible by remember { mutableStateOf(false) }
                                    val avatarOffsetY by animateFloatAsState(targetValue = if (avatarVisible) 0f else 40f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "avatarSlideUp")
                                    val avatarAlpha by animateFloatAsState(targetValue = if (avatarVisible) 1f else 0f, animationSpec = tween(500), label = "avatarFadeIn")
                                    LaunchedEffect(displayHabbo) { delay(300); avatarVisible = true }
                                    val habboGreetings = remember { listOf("Habbo!", "Pool's closed! 🚫", "What are you doing in my room?", "Trade me! 💰", "Bobba! 🚿", "Free furni? 🪑", "Nice room! ✨", "BB Habbo! 👋") }
                                    var currentGreeting by remember { mutableStateOf(habboGreetings.random()) }
                                    var greetingVisible by remember { mutableStateOf(false) }
                                    LaunchedEffect(displayHabbo) { delay(1200); while (true) { greetingVisible = true; delay(3500); greetingVisible = false; delay(600); currentGreeting = habboGreetings.random() } }
                                    var currentPose by remember { mutableStateOf("std") }
                                    val poses = listOf("std" to "🚶 Stand", "wav" to "👋 Wave", "sit" to "🪑 Sit")
                                    val regionFlag = remember(displayHabboRegion) {
                                        when {
                                            displayHabboRegion.endsWith(".es") -> "🇪🇸"; displayHabboRegion.endsWith(".com.br") -> "🇧🇷"
                                            displayHabboRegion.endsWith(".fi") -> "🇫🇮"; displayHabboRegion.endsWith(".de") -> "🇩🇪"
                                            displayHabboRegion.endsWith(".fr") -> "🇫🇷"; displayHabboRegion.endsWith(".it") -> "🇮🇹"
                                            displayHabboRegion.endsWith(".nl") -> "🇳🇱"; displayHabboRegion.endsWith(".se") -> "🇸🇪"
                                            displayHabboRegion.endsWith(".dk") -> "🇩🇰"; displayHabboRegion.endsWith(".no") -> "🇳🇴"
                                            displayHabboRegion.endsWith(".tr") -> "🇹🇷"; else -> "🇺🇸"
                                        }
                                    }
                                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ScrapbookDark).border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = habboNeonAlpha), ScrapbookYellow.copy(alpha = 0.2f), ScrapbookYellow.copy(alpha = habboNeonAlpha))), shape = RoundedCornerShape(16.dp))) {
                                        Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(Brush.verticalGradient(colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460)))))
                                        Row(modifier = Modifier.fillMaxWidth().height(200.dp), horizontalArrangement = Arrangement.SpaceEvenly) { repeat(8) { Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(ScrapbookYellow.copy(alpha = 0.04f))) } }
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookYellow.copy(alpha = flickerAlpha * 0.15f)).border(1.5.dp, ScrapbookYellow.copy(alpha = flickerAlpha), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) { Text("🏨 HABBO HOTEL", fontFamily = BangersFontFamily, color = ScrapbookYellow.copy(alpha = flickerAlpha), fontSize = 14.sp, letterSpacing = 1.sp) }
                                                    Text(regionFlag, fontSize = 18.sp)
                                                }
                                                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookYellow.copy(alpha = 0.1f)).border(1.dp, ScrapbookYellow.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).clickable { val url = "https://www.$displayHabboRegion/profile/$displayHabbo"; try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) { } }.padding(horizontal = 10.dp, vertical = 5.dp)) { Text("VISIT ROOM →", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 11.sp) }
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                val floatT = rememberInfiniteTransition(label = "habboFloat")
                                                val floatY by floatT.animateFloat(initialValue = 0f, targetValue = -5f, animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse), label = "habboFloatY")
                                                Box(modifier = Modifier.offset(y = (avatarOffsetY + floatY).dp).graphicsLayer { alpha = avatarAlpha }) {
                                                    Box(modifier = Modifier.size(150.dp).offset(y = 10.dp).clip(CircleShape).background(Brush.radialGradient(colors = listOf(ScrapbookYellow.copy(alpha = habboNeonAlpha * 0.2f), Color.Transparent))))
                                                    Box(modifier = Modifier.size(140.dp).clip(RoundedCornerShape(12.dp)).background(Color.Transparent).clickable { currentPose = "wav" }, contentAlignment = Alignment.BottomCenter) {
                                                        val poseUrl = remember(currentPose, displayHabbo, displayHabboRegion) { "https://www.$displayHabboRegion/habbo-imaging/avatarimage?user=${displayHabbo.trim()}&action=$currentPose&direction=2&head_direction=2&size=l&gesture=sml" }
                                                        AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(poseUrl).crossfade(true).diskCachePolicy(CachePolicy.DISABLED).memoryCachePolicy(CachePolicy.DISABLED).build(), contentDescription = "Habbo Avatar", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize(), error = rememberVectorPainter(image = Icons.Filled.SmartToy), fallback = rememberVectorPainter(image = Icons.Filled.SmartToy))
                                                    }
                                                    LaunchedEffect(currentPose) { if (currentPose == "wav") { delay(2000); currentPose = "std" } }
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(displayHabbo, fontFamily = BangersFontFamily, color = Color.White, fontSize = 22.sp, letterSpacing = 0.5.sp)
                                                    Text("www.$displayHabboRegion", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    AnimatedVisibility(visible = greetingVisible, enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 2 }), exit = fadeOut(tween(300))) {
                                                        Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.1f)).border(1.dp, ScrapbookYellow.copy(alpha = 0.4f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Text("💬", fontSize = 10.sp); Text(currentGreeting, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp) }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        poses.forEach { (pose, label) ->
                                                            val isActive = currentPose == pose
                                                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (isActive) ScrapbookYellow else ScrapbookYellow.copy(alpha = 0.1f)).border(1.dp, ScrapbookYellow.copy(alpha = if (isActive) 1f else 0.3f), RoundedCornerShape(8.dp)).clickable { currentPose = pose }.padding(horizontal = 8.dp, vertical = 5.dp)) {
                                                                Text(label, fontFamily = BangersFontFamily, color = if (isActive) ScrapbookDark else ScrapbookYellow, fontSize = 10.sp)
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookYellow.copy(alpha = 0.1f)).border(1.dp, ScrapbookYellow.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) { Text("🤖 Talking robot", fontFamily = NunitoFontFamily, color = ScrapbookYellow.copy(alpha = 0.8f), fontSize = 10.sp) }
                                                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF0066CC).copy(alpha = 0.2f)).border(1.dp, Color(0xFF0066CC).copy(alpha = 0.5f), RoundedCornerShape(8.dp)).clickable { val url = "https://www.$displayHabboRegion/profile/$displayHabbo"; try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) { } }.padding(horizontal = 8.dp, vertical = 4.dp)) { Text("➕ Add Friend", fontFamily = BangersFontFamily, color = Color(0xFF4499FF), fontSize = 10.sp) }
                                                    }
                                                }
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
                        TopGamesSection(games = displayGames, topGenre = genreScores.maxByOrNull { it.score }?.genre ?: "")
                        Spacer(modifier = Modifier.height(24.dp))
                        ScrapbookSectionHeader("MY TOP 3 SOUNDTRACKS", "🎵")
                        TopSoundtracksSection(soundtracks = displaySoundtracks, currentUserId = currentUser?.uid ?: "")
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
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF8B0000).copy(alpha = 0.12f)).border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookRed.copy(alpha = neonAlpha), ScrapbookRed.copy(alpha = 0.3f), ScrapbookRed.copy(alpha = neonAlpha))), shape = RoundedCornerShape(10.dp)).clickable { authViewModel.signOut() }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
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
// ─── Accordion Section ────────────────────────────────────────────────────────

@Composable
fun AccordionSection(
    emoji: String,
    title: String,
    accentColor: Color = ScrapbookYellow,
    isExpanded: Boolean,
    completionFraction: Float = 0f,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val neonT = rememberInfiniteTransition(label = "accordion_$title")
    val neonAlpha by neonT.animateFloat(initialValue = 0.3f, targetValue = 0.9f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "accordionNeon_$title")
    val arrowRotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, animationSpec = tween(300, easing = EaseInOut), label = "arrowRotation_$title")

    val headerShape = if (isExpanded)
        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
    else
        RoundedCornerShape(12.dp)

    val contentShape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 12.dp, bottomEnd = 12.dp)

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().clip(headerShape).background(ScrapbookDark).border(width = if (isExpanded) 2.dp else 1.5.dp, brush = Brush.linearGradient(colors = listOf(accentColor.copy(alpha = if (isExpanded) neonAlpha else 0.3f), accentColor.copy(alpha = 0.1f), accentColor.copy(alpha = if (isExpanded) neonAlpha else 0.3f))), shape = headerShape).clickable { onToggle() }.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.width(3.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(accentColor.copy(alpha = if (isExpanded) neonAlpha else 0.5f)))
                    Text(emoji, fontSize = 18.sp)
                    Text(title, fontFamily = BangersFontFamily, color = if (isExpanded) accentColor else Color.White.copy(alpha = 0.7f), fontSize = 17.sp, letterSpacing = 0.5.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (completionFraction > 0f) {
                        Box(modifier = Modifier.width(40.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.1f))) {
                            Box(modifier = Modifier.fillMaxWidth(completionFraction).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(accentColor.copy(alpha = 0.8f)))
                        }
                    } else {
                        Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.15f)).border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = accentColor, modifier = Modifier.size(12.dp))
                        }
                    }
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = accentColor.copy(alpha = 0.7f), modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = arrowRotation })
                }
            }
        }
        AnimatedVisibility(visible = isExpanded, enter = expandVertically(animationSpec = tween(300, easing = EaseInOut)) + fadeIn(animationSpec = tween(300)), exit = shrinkVertically(animationSpec = tween(300, easing = EaseInOut)) + fadeOut(animationSpec = tween(200))) {
            Box(modifier = Modifier.fillMaxWidth().clip(contentShape).background(ScrapbookCardWhite).border(width = 1.5.dp, brush = Brush.linearGradient(colors = listOf(accentColor.copy(alpha = 0.4f), accentColor.copy(alpha = 0.1f), accentColor.copy(alpha = 0.4f))), shape = contentShape).padding(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
            }
        }
    }
}

// ─── Retro Terminal Input ─────────────────────────────────────────────────────

@Composable
fun RetroTerminalInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    maxChars: Int = 0,
    autoAt: Boolean = false,
    autoHttps: Boolean = false,
    singleLine: Boolean = true
) {
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("▶ $label", fontFamily = BangersFontFamily, color = ScrapbookYellow.copy(alpha = 0.7f), fontSize = 11.sp, letterSpacing = 1.sp)
            if (maxChars > 0) {
                Text("${value.length}/$maxChars", fontFamily = BangersFontFamily, color = if (value.length > maxChars * 0.8) ScrapbookRed.copy(alpha = 0.8f) else ScrapbookTextMuted, fontSize = 10.sp)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF0D0D1A)).border(1.dp, if (value.isNotBlank()) ScrapbookYellow.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))) {
            OutlinedTextField(
                value = value,
                onValueChange = { newVal ->
                    var processed = newVal
                    if (autoAt && processed.isNotBlank() && !processed.startsWith("@")) processed = "@$processed"
                    if (maxChars > 0 && processed.length > maxChars) processed = processed.take(maxChars)
                    onValueChange(processed)
                },
                singleLine = singleLine,
                textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, cursorColor = ScrapbookYellow, focusedTextColor = Color.White, unfocusedTextColor = Color.White.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ─── Platform Input With Verify ───────────────────────────────────────────────

@Composable
fun PlatformInputWithVerify(
    value: String,
    onValueChange: (String) -> Unit,
    platform: GamingPlatform,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var verifyState by remember { mutableStateOf<String?>(null) }
    val profileUrl = when (platform.name) {
        "PlayStation" -> if (value.isNotBlank()) "https://psnprofiles.com/$value" else null
        "Xbox" -> if (value.isNotBlank()) "https://xboxgamertag.com/search/$value" else null
        "Steam" -> if (value.isNotBlank()) "https://steamcommunity.com/id/$value" else null
        "Nintendo" -> if (value.isNotBlank()) "https://www.nintendo.com/search/#q=$value" else null
        else -> null
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(platform.color.copy(alpha = 0.15f)).border(1.5.dp, platform.color.copy(alpha = 0.5f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Image(painterResource(platform.iconResId), platform.name, modifier = Modifier.size(22.dp))
            }
            RetroTerminalInput(value = value, onValueChange = { onValueChange(it); verifyState = null }, label = platform.name.uppercase(), modifier = Modifier.weight(1f))
            if (value.isNotBlank() && profileUrl != null) {
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(when (verifyState) { "ok" -> Color(0xFF2E7D32).copy(alpha = 0.2f); "fail" -> ScrapbookRed.copy(alpha = 0.2f); else -> platform.color.copy(alpha = 0.15f) }).border(1.dp, when (verifyState) { "ok" -> Color(0xFF4CAF50); "fail" -> ScrapbookRed; else -> platform.color.copy(alpha = 0.5f) }, RoundedCornerShape(8.dp)).clickable { verifyState = "checking"; try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(profileUrl))); verifyState = "ok" } catch (e: Exception) { verifyState = "fail" } }.padding(horizontal = 10.dp, vertical = 9.dp)) {
                    when (verifyState) {
                        "checking" -> ThreeDotsAnimation(color = platform.color, dotSize = 4.dp)
                        "ok" -> Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                        "fail" -> Icon(Icons.Filled.Close, contentDescription = null, tint = ScrapbookRed, modifier = Modifier.size(16.dp))
                        else -> Text("↗", fontFamily = BangersFontFamily, color = platform.color, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// ─── Edit Card (legacy) ───────────────────────────────────────────────────────

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
        OutlinedTextField(value = value, onValueChange = onValueChange, singleLine = true, textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextDark), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScrapbookYellow, unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f), focusedContainerColor = ScrapbookCardWhite, unfocusedContainerColor = ScrapbookCardWhite, cursorColor = ScrapbookDark, focusedTextColor = ScrapbookTextDark, unfocusedTextColor = ScrapbookTextDark), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth())
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

// ─── Platform Bubble ─────────────────────────────────────────────────────────

@Composable
fun PlatformBubble(platform: GamingPlatform, username: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val profileUrl = when (platform.name) {
        "PlayStation" -> "https://psnprofiles.com/$username"
        "Xbox" -> "https://xboxgamertag.com/search/$username"
        "Steam" -> "https://steamcommunity.com/id/$username"
        "Nintendo" -> "https://www.nintendo.com/search/#q=$username"
        else -> null
    }
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (pressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "platformBubbleScale")
    Box(modifier = modifier.scale(scale).clip(RoundedCornerShape(14.dp)).background(ScrapbookDark).border(width = 1.5.dp, brush = Brush.linearGradient(colors = listOf(platform.color.copy(alpha = 0.9f), platform.color.copy(alpha = 0.3f), platform.color.copy(alpha = 0.9f))), shape = RoundedCornerShape(14.dp)).clickable { pressed = true; profileUrl?.let { try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) } catch (e: Exception) { } } }.padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(platform.color.copy(alpha = 0.15f)).border(1.dp, platform.color.copy(alpha = 0.4f), CircleShape), contentAlignment = Alignment.Center) {
                Image(painterResource(platform.iconResId), platform.name, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(platform.name.uppercase(), fontFamily = BangersFontFamily, color = platform.color, fontSize = 10.sp, letterSpacing = 1.sp)
                Text("@$username", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (profileUrl != null) { Icon(Icons.Filled.OpenInBrowser, contentDescription = null, tint = platform.color.copy(alpha = 0.6f), modifier = Modifier.size(14.dp)) }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Platform Input Field (legacy) ───────────────────────────────────────────

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
    val neonT = rememberInfiniteTransition(label = "streamNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.5f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(700, easing = EaseInOut), RepeatMode.Reverse), label = "streamNeonAlpha")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        if (twitchUsername.isNotBlank()) {
            var twitchPressed by remember { mutableStateOf(false) }
            val twitchScale by animateFloatAsState(targetValue = if (twitchPressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "twitchScale")
            Box(modifier = Modifier.scale(twitchScale).clip(RoundedCornerShape(14.dp)).background(Color(0xFF9146FF)).border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(Color.White.copy(alpha = neonAlpha * 0.4f), Color(0xFF9146FF).copy(alpha = 0.2f), Color.White.copy(alpha = neonAlpha * 0.4f))), shape = RoundedCornerShape(14.dp)).clickable { twitchPressed = true; try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.twitch.tv/$twitchUsername"))) } catch (e: Exception) { } }.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White.copy(alpha = neonAlpha)))
                    Column {
                        Text("TWITCH", fontFamily = BangersFontFamily, color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, letterSpacing = 1.sp)
                        Text("@$twitchUsername", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    }
                    Icon(Icons.Filled.OpenInBrowser, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                }
            }
            LaunchedEffect(twitchPressed) { if (twitchPressed) { delay(150); twitchPressed = false } }
        }
        if (youtubeUsername.isNotBlank()) {
            var ytPressed by remember { mutableStateOf(false) }
            val ytScale by animateFloatAsState(targetValue = if (ytPressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "ytScale")
            Box(modifier = Modifier.scale(ytScale).clip(RoundedCornerShape(14.dp)).background(Color(0xFFCC0000)).border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.3f), Color(0xFFFF4444).copy(alpha = 0.2f), Color.White.copy(alpha = 0.3f))), shape = RoundedCornerShape(14.dp)).clickable { ytPressed = true; try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/@$youtubeUsername"))) } catch (e: Exception) { } }.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Text("▶", color = Color.White, fontSize = 10.sp) }
                    Column {
                        Text("YOUTUBE", fontFamily = BangersFontFamily, color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, letterSpacing = 1.sp)
                        Text("@$youtubeUsername", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    }
                    Icon(Icons.Filled.OpenInBrowser, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                }
            }
            LaunchedEffect(ytPressed) { if (ytPressed) { delay(150); ytPressed = false } }
        }
    }
}

// ─── Bio Card ─────────────────────────────────────────────────────────────────

@Composable
fun BioCard(bioText: String, profilePicUrl: String? = null, habboUsername: String = "", habboRegion: String = "habbo.com") {
    val neonT = rememberInfiniteTransition(label = "bioNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.3f, targetValue = 0.8f, animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOut), RepeatMode.Reverse), label = "bioNeonAlpha")
    var displayedText by remember { mutableStateOf("") }
    var typewriterDone by remember { mutableStateOf(false) }
    LaunchedEffect(bioText) {
        displayedText = ""; typewriterDone = false
        for (i in bioText.indices) { displayedText = bioText.substring(0, i + 1); delay(18L) }
        typewriterDone = true
    }
    val bioTags = remember(bioText) {
        val tags = mutableListOf<Pair<String, String>>()
        val lower = bioText.lowercase()
        if (lower.contains("retro") || lower.contains("vintage")) tags.add("🕹️" to "RETRO")
        if (lower.contains("gamer") || lower.contains("gaming") || lower.contains("game")) tags.add("🎮" to "GAMER")
        if (lower.contains("music") || lower.contains("soundtrack") || lower.contains("ost")) tags.add("🎵" to "MUSIC LOVER")
        if (lower.contains("pixel") || lower.contains("art")) tags.add("🖼️" to "PIXEL ART")
        if (lower.contains("habbo")) tags.add("🏨" to "HABBO")
        if (lower.contains("collector")) tags.add("📦" to "COLLECTOR")
        if (lower.contains("streamer") || lower.contains("twitch") || lower.contains("youtube")) tags.add("📺" to "STREAMER")
        if (lower.contains("developer") || lower.contains("dev") || lower.contains("coder")) tags.add("💻" to "DEVELOPER")
        tags.take(4)
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().offset(x = 3.dp, y = 3.dp).clip(RoundedCornerShape(14.dp)).background(ScrapbookBorder.copy(alpha = 0.15f)))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ScrapbookCardWhite).border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha * 0.6f), ScrapbookBorder.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha * 0.6f))), shape = RoundedCornerShape(14.dp))) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(ScrapbookDark, Color(0xFF2A2A4A), ScrapbookDark))).padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.width(3.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
                        Text("ABOUT ME", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 16.sp, letterSpacing = 2.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) { repeat(3) { Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(ScrapbookYellow.copy(alpha = 0.4f))) } }
                    }
                }
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("\"", fontFamily = BangersFontFamily, color = ScrapbookYellow.copy(alpha = 0.2f), fontSize = 72.sp, lineHeight = 52.sp, modifier = Modifier.offset(y = (-8).dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = if (typewriterDone) bioText else displayedText, fontFamily = NunitoFontFamily, color = ScrapbookTextDark, fontSize = 15.sp, lineHeight = 23.sp)
                        if (bioTags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                bioTags.forEach { (emoji, label) ->
                                    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(ScrapbookDark).border(1.dp, ScrapbookYellow.copy(alpha = 0.3f), RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) { Text(emoji, fontSize = 10.sp); Text(label, fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 9.sp) }
                                    }
                                }
                            }
                        }
                    }
                }
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
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookRed.copy(alpha = 0.15f)).border(1.dp, ScrapbookRed, RoundedCornerShape(6.dp)).clickable { onUnpin() }.padding(horizontal = 10.dp, vertical = 4.dp)) { Text("UNPIN", fontFamily = BangersFontFamily, color = ScrapbookRed, fontSize = 13.sp) }
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
        isLoading -> Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { ThreeDotsAnimation(); Text("Loading activity...", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp) } }
        activities.isEmpty() -> Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), contentAlignment = Alignment.Center) { Text("No activity yet!\nStart bookmarking or writing articles.", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 20.sp) }
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
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(activityIcon, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text(activity.description, fontFamily = NunitoFontFamily, color = ScrapbookTextDark, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(activity.timeAgo, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
                    }
                }
                if (activity.type == "ARTICLE" && activity.itemTitle.isNotBlank()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(start = 64.dp, end = 12.dp, bottom = 12.dp).clip(RoundedCornerShape(8.dp)).background(ScrapbookPaper).border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)).clickable { onArticleClick?.invoke(activity) }.padding(10.dp)) {
                        Column { Text(activity.itemTitle, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis); Spacer(modifier = Modifier.height(4.dp)); Text("READ ARTICLE →", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp) }
                    }
                }
            }
        }
    }
}

// ─── Streak Section ───────────────────────────────────────────────────────────

@Composable
fun ActivityStreakSection(activities: List<ActivityItem>, joinedDate: Long) {
    val streakDays = remember(activities) { if (activities.isEmpty()) 0 else (activities.size / 2).coerceIn(1, 30) }
    val longestStreak = remember(activities) { (streakDays + (0..5).random()).coerceIn(streakDays, 60) }
    val heatmapData = remember(activities) { List(49) { index -> when { index % 7 == 0 -> 0f; activities.size > index / 3 -> (0.4f + (index % 3) * 0.2f).coerceIn(0f, 1f); index % 5 == 0 -> 0.6f; index % 3 == 0 -> 0.3f; else -> 0f } } }
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { listOf("M","T","W","T","F","S","S").forEach { day -> Text(day, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) } }
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
                Text(emoji, fontSize = 20.sp); Spacer(modifier = Modifier.height(4.dp))
                Text(value, fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 24.sp, textAlign = TextAlign.Center)
                Text(label, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp, textAlign = TextAlign.Center, lineHeight = 12.sp)
            }
        }
    }
}
// ─── Game Detail Bottom Sheet ─────────────────────────────────────────────────

@Composable
fun GameDetailBottomSheet(game: Game, index: Int, topGenre: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val accentColor = gameAccentColor(game.name)
    val genre = detectGenreForGame(game.name)
    val isTopGenreMatch = genre.equals(topGenre, ignoreCase = true)
    var igdbData by remember { mutableStateOf<IGDBGame?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(game.name) { try { val results = IGDBRepository.searchGames(game.name); igdbData = results.firstOrNull() } catch (e: Exception) { }; isLoading = false }
    val neonT = rememberInfiniteTransition(label = "sheetNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "sheetNeonAlpha")

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable { onDismiss() }, contentAlignment = Alignment.BottomCenter) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.75f).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(ScrapbookDark).border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(accentColor.copy(alpha = neonAlpha), accentColor.copy(alpha = 0.2f), accentColor.copy(alpha = neonAlpha))), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).clickable { }) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                    val coverUrl = igdbData?.coverUrl ?: game.coverUrl
                    if (!coverUrl.isNullOrBlank()) { AsyncImage(model = coverUrl, contentDescription = game.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                    else if (game.imageResId != null) { Image(painterResource(game.imageResId), game.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                    else { Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(accentColor.copy(alpha = 0.4f), ScrapbookDark))), contentAlignment = Alignment.Center) { Text(game.name.take(2).uppercase(), fontFamily = BangersFontFamily, color = accentColor, fontSize = 56.sp) } }
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, ScrapbookDark.copy(alpha = 0.95f)))))
                    Column(modifier = Modifier.fillMaxSize()) { repeat(40) { Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Color.Black.copy(alpha = 0.06f))); Spacer(modifier = Modifier.height(2.dp)) } }
                    Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp).width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.3f)))
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(32.dp).clip(CircleShape).background(ScrapbookDark.copy(alpha = 0.8f)).clickable { onDismiss() }, contentAlignment = Alignment.Center) { Text("✕", color = Color.White, fontSize = 14.sp) }
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            val medalText = when (index) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "#${index + 1}" }
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(accentColor.copy(alpha = 0.2f)).border(1.dp, accentColor, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) { Text(medalText, fontFamily = BangersFontFamily, color = accentColor, fontSize = 14.sp) }
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(accentColor.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp)) { Text(genre, fontFamily = BangersFontFamily, color = accentColor, fontSize = 12.sp) }
                            if (isTopGenreMatch) { Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookYellow.copy(alpha = 0.2f)).border(1.dp, ScrapbookYellow, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) { Text("⭐ DEFINES YOU", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 11.sp) } }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(game.name, fontFamily = BangersFontFamily, color = Color.White, fontSize = 26.sp, lineHeight = 30.sp)
                    }
                }
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isLoading) { Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { ThreeDotsAnimation() } }
                    else {
                        igdbData?.let { data ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                data.rating?.let { rating -> Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(accentColor.copy(alpha = 0.15f)).border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${rating.toInt()}", fontFamily = BangersFontFamily, color = accentColor, fontSize = 28.sp); Text("RATING", fontFamily = BangersFontFamily, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, letterSpacing = 1.sp) } } }
                                data.releaseYear?.let { year -> Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.05f)).border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$year", fontFamily = BangersFontFamily, color = Color.White, fontSize = 28.sp); Text("RELEASED", fontFamily = BangersFontFamily, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, letterSpacing = 1.sp) } } }
                            }
                            data.summary?.let { summary ->
                                Spacer(modifier = Modifier.height(14.dp))
                                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.05f)).border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp)).padding(14.dp)) {
                                    Column { Text("ABOUT", fontFamily = BangersFontFamily, color = accentColor, fontSize = 13.sp, letterSpacing = 1.sp); Spacer(modifier = Modifier.height(6.dp)); Text(summary, fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, lineHeight = 21.sp, maxLines = 5, overflow = TextOverflow.Ellipsis) }
                                }
                            }
                        } ?: Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.04f)).padding(16.dp), contentAlignment = Alignment.Center) { Text("No database info found", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp) }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(accentColor.copy(alpha = 0.15f)).border(width = 1.5.dp, color = accentColor.copy(alpha = 0.6f), shape = RoundedCornerShape(10.dp)).clickable { val url = "https://www.igdb.com/search?q=${game.name}"; try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) { } }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Filled.OpenInBrowser, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp)); Text("VIEW IN DATABASE →", fontFamily = BangersFontFamily, color = accentColor, fontSize = 15.sp, letterSpacing = 1.sp) }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// ─── Hero Game Card (#1) ──────────────────────────────────────────────────────

@Composable
fun HeroGameCard(game: Game, topGenre: String, onTap: () -> Unit) {
    val accentColor = gameAccentColor(game.name)
    val genre = detectGenreForGame(game.name)
    val neonT = rememberInfiniteTransition(label = "heroGameNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOut), RepeatMode.Reverse), label = "heroGameNeonAlpha")
    val kbT = rememberInfiniteTransition(label = "heroKB")
    val kbScale by kbT.animateFloat(initialValue = 1.0f, targetValue = 1.08f, animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse), label = "heroKBScale")
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "heroPressScale")

    Box(modifier = Modifier.fillMaxWidth().scale(pressScale).clip(RoundedCornerShape(16.dp)).border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(accentColor.copy(alpha = neonAlpha), accentColor.copy(alpha = 0.2f), accentColor.copy(alpha = neonAlpha))), shape = RoundedCornerShape(16.dp)).clickable { pressed = true; onTap() }) {
        Box(modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp))) {
            when {
                game.coverUrl != null -> AsyncImage(model = game.coverUrl, contentDescription = game.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().scale(kbScale))
                game.imageResId != null -> Image(painterResource(game.imageResId), game.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().scale(kbScale))
                else -> Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(accentColor.copy(alpha = 0.5f), ScrapbookDark))), contentAlignment = Alignment.Center) { Text(game.name.take(2).uppercase(), fontFamily = BangersFontFamily, color = accentColor, fontSize = 52.sp) }
            }
            Column(modifier = Modifier.fillMaxSize()) { repeat(35) { Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.Black.copy(alpha = 0.04f))); Spacer(modifier = Modifier.height(2.dp)) } }
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
            Box(modifier = Modifier.align(Alignment.TopStart).padding(10.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFFFD700).copy(alpha = 0.9f)).border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Text("👑", fontSize = 12.sp); Text("#1 FAVORITE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp) } }
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).clip(RoundedCornerShape(8.dp)).background(accentColor.copy(alpha = 0.85f)).padding(horizontal = 8.dp, vertical = 4.dp)) { Text(genre, fontFamily = BangersFontFamily, color = Color.White, fontSize = 11.sp) }
            if (genre.equals(topGenre, ignoreCase = true)) { Box(modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).clip(RoundedCornerShape(8.dp)).background(ScrapbookYellow.copy(alpha = 0.9f)).padding(horizontal = 8.dp, vertical = 4.dp)) { Text("⭐ DEFINES YOU", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 10.sp) } }
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Text(game.name, fontFamily = BangersFontFamily, color = Color.White, fontSize = 24.sp, letterSpacing = 0.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("TAP FOR DETAILS →", fontFamily = BangersFontFamily, color = accentColor.copy(alpha = 0.8f), fontSize = 11.sp, letterSpacing = 1.sp)
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(180); pressed = false } }
}

// ─── Game Item (cards 2-6) ────────────────────────────────────────────────────

@Composable
fun GameItem(game: Game, index: Int = 0, topGenre: String = "", onTap: () -> Unit = {}, modifier: Modifier = Modifier) {
    val accentColor = gameAccentColor(game.name)
    val genre = detectGenreForGame(game.name)
    val isPersonalityMatch = genre.equals(topGenre, ignoreCase = true)
    val neonT = rememberInfiniteTransition(label = "gameNeon_$index")
    val neonAlpha by neonT.animateFloat(initialValue = 0.3f, targetValue = 0.9f, animationSpec = infiniteRepeatable(tween(1600 + index * 200, easing = EaseInOut), RepeatMode.Reverse), label = "gameNeonAlpha_$index")
    val kbT = rememberInfiniteTransition(label = "gameKB_$index")
    val kbScale by kbT.animateFloat(initialValue = 1.05f, targetValue = 1.14f, animationSpec = infiniteRepeatable(keyframes { durationMillis = 12000 + index * 1500; 1.05f at 0; 1.14f at (6000 + index * 750); 1.05f at (12000 + index * 1500) }, RepeatMode.Restart), label = "gameKBScale_$index")
    var visible by remember { mutableStateOf(false) }
    val enterOffset by animateFloatAsState(targetValue = if (visible) 0f else 50f, animationSpec = tween(500, delayMillis = index * 80, easing = LinearOutSlowInEasing), label = "gameEnterOffset_$index")
    val enterAlpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(500, delayMillis = index * 80, easing = LinearOutSlowInEasing), label = "gameEnterAlpha_$index")
    LaunchedEffect(Unit) { visible = true }
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(targetValue = if (pressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "gamePress_$index")

    Box(modifier = modifier.offset(y = enterOffset.dp).graphicsLayer { alpha = enterAlpha }.scale(pressScale).clip(RoundedCornerShape(12.dp)).border(width = if (isPersonalityMatch) 2.dp else 1.5.dp, brush = Brush.linearGradient(colors = listOf(accentColor.copy(alpha = if (isPersonalityMatch) neonAlpha else neonAlpha * 0.6f), accentColor.copy(alpha = 0.1f), accentColor.copy(alpha = if (isPersonalityMatch) neonAlpha else neonAlpha * 0.6f))), shape = RoundedCornerShape(12.dp)).clickable { pressed = true; onTap() }) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp, shadowOffset = 4.dp) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(GAME_IMAGE_HEIGHT).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))) {
                    when {
                        game.coverUrl != null -> AsyncImage(model = game.coverUrl, contentDescription = game.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().scale(kbScale))
                        game.imageResId != null -> Image(painterResource(game.imageResId), game.name, modifier = Modifier.fillMaxSize().scale(kbScale), contentScale = ContentScale.Crop)
                        else -> Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(accentColor.copy(alpha = 0.4f), ScrapbookDark))), contentAlignment = Alignment.Center) { Text(game.name.take(2).uppercase(), fontFamily = BangersFontFamily, color = accentColor, fontSize = 32.sp) }
                    }
                    Column(modifier = Modifier.fillMaxSize()) { repeat(20) { Box(modifier = Modifier.fillMaxWidth().height(5.dp).background(Color.Black.copy(alpha = 0.04f))); Spacer(modifier = Modifier.height(3.dp)) } }
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)))))
                    val medalText = when (index + 1) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "${index + 1}" }
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(8.dp)).background(ScrapbookDark.copy(alpha = 0.85f)).border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 3.dp)) { Text(medalText, fontFamily = BangersFontFamily, color = accentColor, fontSize = 12.sp) }
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(6.dp)).background(accentColor.copy(alpha = 0.85f)).padding(horizontal = 6.dp, vertical = 3.dp)) { Text(genre, fontFamily = BangersFontFamily, color = Color.White, fontSize = 8.sp) }
                    if (isPersonalityMatch) { Box(modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).clip(RoundedCornerShape(6.dp)).background(ScrapbookYellow.copy(alpha = 0.9f)).padding(horizontal = 5.dp, vertical = 2.dp)) { Text("⭐", fontSize = 10.sp) } }
                }
                Box(modifier = Modifier.fillMaxWidth().height(GAME_LABEL_HEIGHT).background(Brush.horizontalGradient(colors = listOf(accentColor.copy(alpha = 0.15f), ScrapbookDark, accentColor.copy(alpha = 0.15f)))).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                    Text(game.name, fontFamily = BangersFontFamily, color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(180); pressed = false } }
}

// ─── Top Games Section ────────────────────────────────────────────────────────

@Composable
fun TopGamesSection(games: List<Game>, topGenre: String = "") {
    if (games.isEmpty()) return
    var selectedGameIndex by remember { mutableStateOf<Int?>(null) }
    var layoutIsGrid by remember { mutableStateOf(true) }
    val neonT = rememberInfiniteTransition(label = "gamesNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.3f, targetValue = 0.8f, animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse), label = "gamesNeonAlpha")

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.End) {
            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(ScrapbookDark).border(1.dp, ScrapbookYellow.copy(alpha = neonAlpha * 0.5f), RoundedCornerShape(20.dp)).padding(2.dp)) {
                Row {
                    listOf(true to "⊞ GRID", false to "☰ LIST").forEach { (isGrid, label) ->
                        Box(modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(if (layoutIsGrid == isGrid) ScrapbookYellow else Color.Transparent).clickable { layoutIsGrid = isGrid }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(label, fontFamily = BangersFontFamily, color = if (layoutIsGrid == isGrid) ScrapbookDark else Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        val gameList = games.take(6)
        if (layoutIsGrid) {
            if (gameList.isNotEmpty()) { HeroGameCard(game = gameList[0], topGenre = topGenre, onTap = { selectedGameIndex = 0 }); Spacer(modifier = Modifier.height(12.dp)) }
            gameList.drop(1).chunked(2).forEachIndexed { rowIndex, rowGames ->
                if (rowIndex > 0) Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowGames.forEachIndexed { colIndex, game -> GameItem(game = game, index = rowIndex * 2 + colIndex + 1, topGenre = topGenre, onTap = { selectedGameIndex = rowIndex * 2 + colIndex + 1 }, modifier = Modifier.weight(1f)) }
                    if (rowGames.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        } else {
            gameList.forEachIndexed { index, game ->
                if (index > 0) Spacer(modifier = Modifier.height(10.dp))
                val accentColor = gameAccentColor(game.name)
                val genre = detectGenreForGame(game.name)
                val isMatch = genre.equals(topGenre, ignoreCase = true)
                var listVisible by remember { mutableStateOf(false) }
                val listOffset by animateFloatAsState(targetValue = if (listVisible) 0f else 40f, animationSpec = tween(400, delayMillis = index * 60, easing = LinearOutSlowInEasing), label = "listOffset_$index")
                val listAlpha by animateFloatAsState(targetValue = if (listVisible) 1f else 0f, animationSpec = tween(400, delayMillis = index * 60), label = "listAlpha_$index")
                LaunchedEffect(Unit) { listVisible = true }
                val medalText = when (index + 1) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "#${index + 1}" }
                var pressed by remember { mutableStateOf(false) }
                val pressScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "listPress_$index")

                Box(modifier = Modifier.offset(y = listOffset.dp).graphicsLayer { alpha = listAlpha }.scale(pressScale).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ScrapbookDark).border(1.5.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).clickable { pressed = true; selectedGameIndex = index }.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(accentColor.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            when {
                                game.coverUrl != null -> AsyncImage(model = game.coverUrl, contentDescription = game.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)))
                                game.imageResId != null -> Image(painterResource(game.imageResId), game.name, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                else -> Text(game.name.take(2).uppercase(), fontFamily = BangersFontFamily, color = accentColor, fontSize = 18.sp)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(game.name, fontFamily = BangersFontFamily, color = Color.White, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(medalText, fontSize = 12.sp)
                                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(accentColor.copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text(genre, fontFamily = BangersFontFamily, color = accentColor, fontSize = 9.sp) }
                                if (isMatch) Text("⭐", fontSize = 10.sp)
                            }
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = accentColor.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    }
                }
                LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
            }
        }
    }

    selectedGameIndex?.let { idx ->
        val game = games.getOrNull(idx)
        if (game != null) { GameDetailBottomSheet(game = game, index = idx, topGenre = topGenre, onDismiss = { selectedGameIndex = null }) }
    }
}

// ─── Soundtrack Reaction Bar ──────────────────────────────────────────────────

@Composable
fun SoundtrackReactionBar(soundtrackTitle: String, currentUserId: String) {
    val initialReactions: Map<String, Int> = mapOf("🔥" to 0, "❤️" to 0, "🎵" to 0)
    var reactions by remember { mutableStateOf(initialReactions) }
    var userReacted by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(soundtrackTitle) {
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val doc = db.collection("soundtrackReactions").document(soundtrackTitle.hashCode().toString()).get().await()
            if (doc.exists()) {
                val data = doc.data ?: emptyMap<String, Any>()
                reactions = mapOf<String, Int>(
                    "🔥" to ((data["fire"] as? Long)?.toInt() ?: 0),
                    "❤️" to ((data["heart"] as? Long)?.toInt() ?: 0),
                    "🎵" to ((data["music"] as? Long)?.toInt() ?: 0)
                )
                userReacted = data["user_$currentUserId"] as? String
            }
        } catch (e: Exception) { }
    }
    val emojiList = listOf("🔥", "❤️", "🎵")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        emojiList.forEach { emoji ->
            val count = reactions[emoji] ?: 0
            val isReacted = userReacted == emoji
            var popped by remember { mutableStateOf(false) }
            val popScale by animateFloatAsState(targetValue = if (popped) 1.4f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh), label = "soundtrackPop_$emoji")
            Box(modifier = Modifier.scale(popScale).clip(RoundedCornerShape(20.dp)).background(if (isReacted) ScrapbookYellow.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)).border(1.5.dp, if (isReacted) ScrapbookYellow.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp)).clickable {
                popped = true
                val newReacted = if (userReacted == emoji) null else emoji
                val prev = userReacted
                userReacted = newReacted
                reactions = reactions.toMutableMap().apply { prev?.let { this[it] = (this[it] ?: 1) - 1 }; newReacted?.let { this[it] = (this[it] ?: 0) + 1 } }
                try {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val docRef = db.collection("soundtrackReactions").document(soundtrackTitle.hashCode().toString())
                    val updates = mutableMapOf<String, Any>("fire" to (reactions["🔥"] ?: 0), "heart" to (reactions["❤️"] ?: 0), "music" to (reactions["🎵"] ?: 0), "title" to soundtrackTitle)
                    if (currentUserId.isNotBlank()) updates["user_$currentUserId"] = newReacted ?: ""
                    docRef.set(updates, com.google.firebase.firestore.SetOptions.merge())
                } catch (e: Exception) { }
            }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(emoji, fontSize = 14.sp)
                    if (count > 0) Text("$count", fontFamily = BangersFontFamily, color = if (isReacted) ScrapbookYellow else Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }
            LaunchedEffect(popped) { if (popped) { delay(200); popped = false } }
        }
    }
}

// ─── Soundtrack Detail Bottom Sheet ──────────────────────────────────────────

@Composable
fun SoundtrackDetailBottomSheet(soundtrack: Soundtrack, index: Int, currentUserId: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var igdbData by remember { mutableStateOf<IGDBGame?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(soundtrack.title) { try { val query = soundtrack.artist ?: soundtrack.title.replace(" OST", ""); val results = IGDBRepository.searchGames(query); igdbData = results.firstOrNull() } catch (e: Exception) { }; isLoading = false }
    val neonT = rememberInfiniteTransition(label = "stSheetNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "stSheetNeonAlpha")
    val spinT = rememberInfiniteTransition(label = "stSheetSpin")
    val spinAngle by spinT.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart), label = "stSheetSpinAngle")

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)).clickable { onDismiss() }, contentAlignment = Alignment.BottomCenter) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.82f).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(Brush.verticalGradient(colors = listOf(Color(0xFF1A1A2E), Color(0xFF0D0D1A)))).border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.15f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).clickable { }) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp).width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.2f)))
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(220.dp).clip(CircleShape).background(Brush.radialGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha * 0.25f), Color.Transparent))))
                    Box(modifier = Modifier.size(200.dp).graphicsLayer { rotationZ = spinAngle }.clip(CircleShape).background(Color(0xFF111111)), contentAlignment = Alignment.Center) {
                        listOf(0.98f, 0.88f, 0.78f, 0.68f, 0.58f).forEach { f -> Box(modifier = Modifier.size((200 * f).dp).clip(CircleShape).border(0.5.dp, Color.White.copy(alpha = 0.05f), CircleShape)) }
                        when { soundtrack.coverUrl != null -> AsyncImage(model = soundtrack.coverUrl, contentDescription = soundtrack.title, contentScale = ContentScale.Crop, modifier = Modifier.size(200.dp).clip(CircleShape), alpha = 0.8f); soundtrack.imageResId != null -> Image(painterResource(soundtrack.imageResId), soundtrack.title, modifier = Modifier.size(200.dp).clip(CircleShape), contentScale = ContentScale.Crop, alpha = 0.8f) }
                        Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.Black.copy(alpha = 0.3f)))
                        Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0f)))))
                    }
                    Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF0D0D1A)).border(2.dp, ScrapbookYellow.copy(alpha = 0.5f), CircleShape))
                    Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = (-10).dp, y = 20.dp).width(3.dp).height(90.dp).graphicsLayer { rotationZ = -25f }.background(Brush.verticalGradient(colors = listOf(Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0.3f)))))
                    Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = (-6).dp, y = 105.dp).size(8.dp).clip(CircleShape).background(ScrapbookYellow))
                }
                Spacer(modifier = Modifier.height(20.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val dotT = rememberInfiniteTransition(label = "nowPlayingDot")
                        val dotAlpha by dotT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(600, easing = EaseInOut), RepeatMode.Reverse), label = "dotAlpha")
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ScrapbookYellow.copy(alpha = dotAlpha)))
                        Text("NOW PLAYING", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 13.sp, letterSpacing = 2.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                            repeat(5) { i ->
                                val eqT = rememberInfiniteTransition(label = "eq_$i")
                                val eqH by eqT.animateFloat(initialValue = 4f, targetValue = 16f, animationSpec = infiniteRepeatable(tween(300 + i * 80, easing = EaseInOut), RepeatMode.Reverse), label = "eqBar_$i")
                                Box(modifier = Modifier.width(3.dp).height(eqH.dp).clip(RoundedCornerShape(1.dp)).background(ScrapbookYellow.copy(alpha = 0.8f)))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(soundtrack.title, fontFamily = BangersFontFamily, color = Color.White, fontSize = 26.sp, lineHeight = 30.sp)
                    soundtrack.artist?.let { Text(it, fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.55f), fontSize = 14.sp) }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isLoading) { Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) { ThreeDotsAnimation() } }
                    else {
                        igdbData?.let { data ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                data.rating?.let { rating -> Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(ScrapbookYellow.copy(alpha = 0.1f)).border(1.dp, ScrapbookYellow.copy(alpha = 0.4f), RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${rating.toInt()}", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 26.sp); Text("RATING", fontFamily = BangersFontFamily, color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, letterSpacing = 1.sp) } } }
                                data.releaseYear?.let { year -> Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.05f)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$year", fontFamily = BangersFontFamily, color = Color.White, fontSize = 26.sp); Text("RELEASED", fontFamily = BangersFontFamily, color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, letterSpacing = 1.sp) } } }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("REACTIONS", fontFamily = BangersFontFamily, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, letterSpacing = 1.sp)
                        SoundtrackReactionBar(soundtrackTitle = soundtrack.title, currentUserId = currentUserId)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ScrapbookYellow.copy(alpha = 0.12f)).border(1.5.dp, ScrapbookYellow.copy(alpha = 0.5f), RoundedCornerShape(10.dp)).clickable { val query = "${soundtrack.title} game soundtrack".replace(" ", "+"); val url = "https://archive.org/search?query=$query"; try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) { } }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("🎵", fontSize = 16.sp); Text("LISTEN ON ARCHIVE.ORG →", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 14.sp, letterSpacing = 1.sp) }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }
    }
}

// ─── Soundtrack Item ──────────────────────────────────────────────────────────

@Composable
fun SoundtrackItem(soundtrack: Soundtrack, index: Int = 0, currentUserId: String = "", modifier: Modifier = Modifier) {
    var showDetail by remember { mutableStateOf(false) }
    var isFlipped by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var lastTapTime by remember { mutableStateOf(0L) }

    // Always spinning — faster when playing
    val spinT = rememberInfiniteTransition(label = "vinyl_$index")
    val spinAngle by spinT.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(if (isPlaying) 3000 else 8000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "vinylAngle_$index"
    )

    val glowT = rememberInfiniteTransition(label = "vinylGlow_$index")
    val glowAlpha by glowT.animateFloat(
        initialValue = 0.2f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1200 + index * 200, easing = EaseInOut), RepeatMode.Reverse),
        label = "vinylGlowAlpha_$index"
    )
    val neonT = rememberInfiniteTransition(label = "stNeon_$index")
    val neonAlpha by neonT.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse),
        label = "stNeonAlpha_$index"
    )
    val flipRotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(500, easing = EaseInOut),
        label = "flip_$index"
    )

    // Faster staggered entrance
    var visible by remember { mutableStateOf(false) }
    val enterOffset by animateFloatAsState(
        targetValue = if (visible) 0f else 30f,
        animationSpec = tween(350, delayMillis = index * 60, easing = LinearOutSlowInEasing),
        label = "stEnter_$index"
    )
    val enterAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300, delayMillis = index * 60),
        label = "stEnterAlpha_$index"
    )
    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = modifier
            .width(160.dp)
            .offset(y = enterOffset.dp)
            .graphicsLayer { alpha = enterAlpha }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Reserved height badge row — keeps all items aligned
        Box(modifier = Modifier.height(28.dp), contentAlignment = Alignment.Center) {
            if (index == 0) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ScrapbookDark)
                        .border(1.dp, ScrapbookYellow.copy(alpha = neonAlpha), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val dotT = rememberInfiniteTransition(label = "nowDot_$index")
                    val dotA by dotT.animateFloat(
                        initialValue = 0.3f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(500, easing = EaseInOut), RepeatMode.Reverse),
                        label = "nowDotAlpha_$index"
                    )
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ScrapbookYellow.copy(alpha = dotA)))
                    Text("NOW PLAYING", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 9.sp, letterSpacing = 1.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp), verticalAlignment = Alignment.Bottom) {
                        repeat(4) { i ->
                            val eqT = rememberInfiniteTransition(label = "smallEq_${index}_$i")
                            val eqH by eqT.animateFloat(
                                initialValue = 3f, targetValue = 10f,
                                animationSpec = infiniteRepeatable(tween(280 + i * 70, easing = EaseInOut), RepeatMode.Reverse),
                                label = "smallEqH_${index}_$i"
                            )
                            Box(modifier = Modifier.width(2.dp).height(eqH.dp).clip(RoundedCornerShape(1.dp)).background(ScrapbookYellow.copy(alpha = 0.8f)))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // CD disc
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .graphicsLayer { rotationY = flipRotation }
                .clickable {
                    val now = System.currentTimeMillis()
                    if (now - lastTapTime < 400) {
                        showDetail = true
                    } else {
                        isPlaying = !isPlaying
                    }
                    lastTapTime = now
                }
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { isFlipped = !isFlipped })
                }
        ) {
            // Outer glow
            Box(
                modifier = Modifier
                    .size(155.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPlaying) ScrapbookYellow.copy(alpha = glowAlpha * 0.35f)
                        else Color.White.copy(alpha = glowAlpha * 0.1f)
                    )
            )

            // CD disc body
            Box(
                modifier = Modifier
                    .size(148.dp)
                    .graphicsLayer { rotationZ = spinAngle }
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // CD rainbow surface
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFFCCCCCC),
                                    Color(0xFFE8E8FF),
                                    Color(0xFFFFE8FF),
                                    Color(0xFFE8FFE8),
                                    Color(0xFFE8F8FF),
                                    Color(0xFFFFFFE8),
                                    Color(0xFFFFE8E8),
                                    Color(0xFFCCCCCC)
                                )
                            )
                        )
                )

                // Cover art on CD surface
                when {
                    soundtrack.coverUrl != null -> AsyncImage(
                        model = soundtrack.coverUrl,
                        contentDescription = soundtrack.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(148.dp).clip(CircleShape),
                        alpha = 0.45f
                    )
                    soundtrack.imageResId != null -> Image(
                        painterResource(soundtrack.imageResId),
                        soundtrack.title,
                        modifier = Modifier.size(148.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        alpha = 0.45f
                    )
                }

                // CD shine rings
                listOf(0.95f, 0.82f, 0.65f).forEach { f ->
                    Box(
                        modifier = Modifier
                            .size((148 * f).dp)
                            .clip(CircleShape)
                            .border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    )
                }

                // Shine streak
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0f),
                                    Color.White.copy(alpha = 0.25f),
                                    Color.White.copy(alpha = 0f)
                                )
                            )
                        )
                )

                // Center hole
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A2E))
                        .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                )
            }

            // Playing indicator ring
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(152.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    ScrapbookYellow.copy(alpha = 0f),
                                    ScrapbookYellow.copy(alpha = neonAlpha),
                                    ScrapbookYellow.copy(alpha = 0f)
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }
        }

        // Flipped back
        if (isFlipped) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ScrapbookDark)
                    .border(1.dp, ScrapbookYellow.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("TRACK INFO", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 11.sp, letterSpacing = 1.sp)
                    Text(soundtrack.title, fontFamily = BangersFontFamily, color = Color.White, fontSize = 14.sp)
                    soundtrack.artist?.let { Text("By $it", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp) }
                    Text("▶ Tap = play/stop\n👆 Double tap = details\n📱 Long press = flip", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.35f), fontSize = 9.sp, lineHeight = 14.sp)
                }
            }
        }

        // Front info
        if (!isFlipped) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                soundtrack.title,
                fontFamily = BangersFontFamily,
                color = ScrapbookDark,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            soundtrack.artist?.let {
                Text(
                    it,
                    fontFamily = NunitoFontFamily,
                    color = ScrapbookTextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (isPlaying) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ScrapbookYellow.copy(alpha = 0.15f))
                        .border(1.dp, ScrapbookYellow.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("▶ PLAYING", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 9.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "TAP PLAY · DOUBLE-TAP DETAILS · LONG PRESS FLIP",
                fontFamily = BangersFontFamily,
                color = ScrapbookTextMuted.copy(alpha = 0.4f),
                fontSize = 7.sp,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }

    if (showDetail) {
        SoundtrackDetailBottomSheet(
            soundtrack = soundtrack,
            index = index,
            currentUserId = currentUserId,
            onDismiss = { showDetail = false }
        )
    }
}

// ─── Top Soundtracks Section ──────────────────────────────────────────────────

@Composable
fun TopSoundtracksSection(soundtracks: List<Soundtrack>, currentUserId: String = "") {
    if (soundtracks.isEmpty()) return
    val neonT = rememberInfiniteTransition(label = "stSectionNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.3f, targetValue = 0.8f, animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse), label = "stSectionNeonAlpha")
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 4.dp), horizontalArrangement = Arrangement.End) {            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(ScrapbookDark.copy(alpha = 0.6f)).border(1.dp, ScrapbookYellow.copy(alpha = neonAlpha * 0.4f), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text("👆 TAP  •  2x SPEED  •  LONG PRESS FLIP", fontFamily = BangersFontFamily, color = ScrapbookTextMuted, fontSize = 8.sp, letterSpacing = 0.5.sp)
            }
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            items(soundtracks.take(3).size) { index ->
                SoundtrackItem(
                    soundtrack = soundtracks[index],
                    index = index,
                    currentUserId = currentUserId
                )
            }
        }
    }
}

// ─── Genre Slideshow Data ─────────────────────────────────────────────────────

val genreSlideshowGames = mapOf(
    "RPG" to listOf("Final Fantasy VII", "Final Fantasy X", "Skyrim", "Persona 5", "Chrono Trigger", "Dragon Quest XI"),
    "Action" to listOf("Infamous Second Son", "God of War", "Devil May Cry 5", "Bayonetta", "Sonic the Hedgehog", "Cyberpunk 2077"),
    "Platformer" to listOf("Super Mario World", "Fez", "Hollow Knight", "Crash Bandicoot", "Kirby Super Star", "Celeste"),
    "Shooter" to listOf("Doom", "Halo Combat Evolved", "Half-Life 2", "GoldenEye 007", "Quake", "Metroid Prime"),
    "Adventure" to listOf("Tomb Raider", "Uncharted 4", "Minecraft", "The Legend of Zelda Ocarina of Time", "Shadow of the Colossus", "Ico"),
    "Arcade" to listOf("Pac-Man", "Street Fighter II", "Tetris", "Donkey Kong", "Galaga", "Space Invaders")
)

// ─── Radar Chart with Slideshow Background ────────────────────────────────────

@Composable
fun RetroRadarChart(genres: List<GenreScore>) {
    val animProgress by animateFloatAsState(targetValue = 1f, animationSpec = tween(1400, easing = LinearOutSlowInEasing), label = "radar_anim")
    val topGenre = remember(genres) { genres.maxByOrNull { it.score }?.genre ?: "RPG" }
    var slideCovers by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentSlide by remember { mutableStateOf(0) }
    var slideVisible by remember { mutableStateOf(true) }

    LaunchedEffect(topGenre) {
        val gameNames = genreSlideshowGames[topGenre] ?: genreSlideshowGames["RPG"]!!
        val covers = mutableListOf<String>()
        for (name in gameNames) { try { val result = IGDBRepository.fetchGameCoverByName(name); if (result != null) covers.add(result); if (covers.size >= 5) break } catch (e: Exception) { } }
        slideCovers = covers
    }
    LaunchedEffect(slideCovers) { if (slideCovers.size > 1) { while (true) { delay(3500L); slideVisible = false; delay(400L); currentSlide = (currentSlide + 1) % slideCovers.size; slideVisible = true } } }

    val neonT = rememberInfiniteTransition(label = "radarNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse), label = "radarNeonAlpha")
    val slideAlpha by animateFloatAsState(targetValue = if (slideVisible) 1f else 0f, animationSpec = tween(400, easing = EaseInOut), label = "slideAlpha")
    val genreAccentColor = when (topGenre) { "RPG" -> Color(0xFF8E24AA); "Action" -> Color(0xFFE53935); "Platformer" -> Color(0xFF1565C0); "Shooter" -> Color(0xFF00838F); "Adventure" -> Color(0xFF2E7D32); "Arcade" -> Color(0xFFEF6C00); else -> ScrapbookYellow }

    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookDark, cornerRadius = 14.dp, shadowOffset = 4.dp) {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(genreAccentColor.copy(alpha = neonAlpha), genreAccentColor.copy(alpha = 0.2f), genreAccentColor.copy(alpha = neonAlpha))), shape = RoundedCornerShape(14.dp))) {
                if (slideCovers.isNotEmpty()) {
                    val coverUrl = slideCovers.getOrNull(currentSlide)
                    if (coverUrl != null) { AsyncImage(model = coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(400.dp).graphicsLayer { alpha = slideAlpha * 0.35f }) }
                    Box(modifier = Modifier.fillMaxWidth().height(400.dp).background(Brush.verticalGradient(colors = listOf(ScrapbookDark.copy(alpha = 0.6f), ScrapbookDark.copy(alpha = 0.85f), ScrapbookDark))))
                }
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎯 GAMING PERSONALITY", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 20.sp, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Based on your top games", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(genreAccentColor.copy(alpha = 0.8f)).padding(horizontal = 8.dp, vertical = 3.dp)) { Text("▶ $topGenre", fontFamily = BangersFontFamily, color = Color.White, fontSize = 11.sp) }
                    }
                    if (slideCovers.size > 1) { Spacer(modifier = Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { slideCovers.forEachIndexed { i, _ -> Box(modifier = Modifier.size(if (i == currentSlide) 8.dp else 5.dp).clip(CircleShape).background(if (i == currentSlide) genreAccentColor else Color.White.copy(alpha = 0.3f))) } } }
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(220.dp).padding(16.dp)) {
                        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                        val radius = size.minDimension / 2f
                        val count = genres.size
                        val angleStep = (2 * Math.PI / count).toFloat()
                        listOf(0.25f, 0.5f, 0.75f, 1f).forEach { ring ->
                            val rp = androidx.compose.ui.graphics.Path()
                            for (i in 0 until count) { val angle = i * angleStep - (Math.PI / 2).toFloat(); val x = center.x + radius * ring * kotlin.math.cos(angle); val y = center.y + radius * ring * kotlin.math.sin(angle); if (i == 0) rp.moveTo(x, y) else rp.lineTo(x, y) }
                            rp.close(); drawPath(rp, Color.White.copy(alpha = 0.08f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                        }
                        for (i in 0 until count) { val angle = i * angleStep - (Math.PI / 2).toFloat(); drawLine(Color.White.copy(alpha = 0.1f), center, androidx.compose.ui.geometry.Offset(center.x + radius * kotlin.math.cos(angle), center.y + radius * kotlin.math.sin(angle)), 1.dp.toPx()) }
                        val rp = androidx.compose.ui.graphics.Path()
                        genres.forEachIndexed { i, genre -> val angle = i * angleStep - (Math.PI / 2).toFloat(); val r = radius * genre.score * animProgress; val x = center.x + r * kotlin.math.cos(angle); val y = center.y + r * kotlin.math.sin(angle); if (i == 0) rp.moveTo(x, y) else rp.lineTo(x, y) }
                        rp.close(); drawPath(rp, genreAccentColor.copy(alpha = 0.3f)); drawPath(rp, genreAccentColor, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                        genres.forEachIndexed { i, genre -> val angle = i * angleStep - (Math.PI / 2).toFloat(); val r = radius * genre.score * animProgress; val cx = center.x + r * kotlin.math.cos(angle); val cy = center.y + r * kotlin.math.sin(angle); drawCircle(genreAccentColor, 5.dp.toPx(), androidx.compose.ui.geometry.Offset(cx, cy)); drawCircle(ScrapbookDark, 2.5f.dp.toPx(), androidx.compose.ui.geometry.Offset(cx, cy)) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    genres.chunked(3).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { genre ->
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (genre.genre == topGenre) genreAccentColor else ScrapbookYellow.copy(alpha = 0.5f)))
                                    Text(genre.genre, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = if (genre.genre == topGenre) genreAccentColor else Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                                    Text("${(genre.score * 100).toInt()}%", fontFamily = NunitoFontFamily, color = if (genre.genre == topGenre) genreAccentColor.copy(alpha = 0.8f) else ScrapbookYellow.copy(alpha = 0.5f), fontSize = 10.sp)
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
// ─── Aliases ──────────────────────────────────────────────────────────────────

@Composable fun ProfileSectionHeader(title: String, emoji: String, color: Color) { ScrapbookSectionHeader(title, emoji) }
@Composable fun ProfileSectionTitle(title: String) { Text(title.uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) }
@Composable fun RetroInputField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) { ScrapbookInputField(value, onValueChange, label, modifier) }
@Composable fun XPProgressBar(xp: Int, level: RetroLevel, progress: Float) { ScrapbookXPProgressBar(xp, level, progress, getXpToNextLevel(xp)) }
@Composable fun StatCard(value: String, label: String, color: Color, onClick: () -> Unit) { ScrapbookStatCard(value, label, onClick = onClick) }

@Preview(showBackground = true, backgroundColor = 0xFFFAF3E0)
@Composable
fun ProfileScreenPreview() { HubRetroTheme { ProfileScreen() } }

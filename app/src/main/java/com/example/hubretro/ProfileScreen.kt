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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.hubretro.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val GAME_IMAGE_HEIGHT = 160.dp
private val GAME_LABEL_HEIGHT = 40.dp

data class Soundtrack(val title: String, val artist: String? = null, val imageResId: Int? = null, val coverUrl: String? = null)

// ── Floating retro pixel icons — background effect ───────────────────────────
private val pixelHeart  = listOf(0 to 1, 0 to 2, 0 to 4, 0 to 5, 1 to 0, 1 to 1, 1 to 2, 1 to 3, 1 to 4, 1 to 5, 1 to 6, 2 to 0, 2 to 1, 2 to 2, 2 to 3, 2 to 4, 2 to 5, 2 to 6, 3 to 1, 3 to 2, 3 to 3, 3 to 4, 3 to 5, 4 to 2, 4 to 3, 4 to 4, 5 to 3)
private val pixelStar   = listOf(0 to 2, 1 to 1, 1 to 2, 1 to 3, 2 to 0, 2 to 1, 2 to 2, 2 to 3, 2 to 4, 3 to 1, 3 to 2, 3 to 3, 4 to 0, 4 to 4)
private val pixelCoin   = listOf(0 to 1, 0 to 2, 0 to 3, 1 to 0, 1 to 1, 1 to 2, 1 to 3, 1 to 4, 2 to 0, 2 to 2, 2 to 4, 3 to 0, 3 to 1, 3 to 2, 3 to 3, 3 to 4, 4 to 1, 4 to 2, 4 to 3)
private val pixelDpad   = listOf(0 to 2, 1 to 1, 1 to 2, 1 to 3, 2 to 0, 2 to 1, 2 to 2, 2 to 3, 2 to 4, 3 to 1, 3 to 2, 3 to 3, 4 to 2)
private val pixelBolt   = listOf(0 to 2, 0 to 3, 1 to 1, 1 to 2, 1 to 3, 2 to 0, 2 to 1, 2 to 2, 3 to 1, 3 to 2, 3 to 3, 4 to 2, 4 to 3)
private val pixelGem    = listOf(0 to 2, 1 to 1, 1 to 2, 1 to 3, 2 to 0, 2 to 1, 2 to 2, 2 to 3, 2 to 4, 3 to 1, 3 to 2, 3 to 3, 4 to 2)
private val allPixelShapes = listOf(pixelHeart, pixelStar, pixelCoin, pixelDpad, pixelBolt, pixelGem)

private data class PixelIconDef(
    val xRatio: Float, val yRatio: Float, val speed: Float,
    val shapeIdx: Int, val cellDp: Float, val alpha: Float, val colorIdx: Int
)

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
    val tabs = listOf("PROFILE", "ARTICLES", "BOOKMARKS")
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
    var bookmarkedArticles by remember { mutableStateOf<List<ArticleItem>>(emptyList()) }
    var isLoadingArticles by remember { mutableStateOf(false) }
    var isLoadingBookmarks by remember { mutableStateOf(false) }

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
            isLoadingArticles = true
            try {
                val db = FirebaseFirestore.getInstance()
                val docs = db.collection("articles").whereEqualTo("authorUid", uid)
                    .orderBy("timestamp", Query.Direction.DESCENDING).limit(20).get().await()
                userArticles = docs.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    ArticleItem(id = doc.id, title = data["title"] as? String ?: "", snippet = data["snippet"] as? String ?: "", fullContent = data["fullContent"] as? String ?: "", imageUrl = data["headerImageUrl"] as? String)
                }.filter { it.title.isNotBlank() }
                val pinnedId = db.collection("users").document(uid).get().await().getString("pinnedArticleId")
                if (!pinnedId.isNullOrBlank()) pinnedArticle = userArticles.firstOrNull { it.id == pinnedId }
            } catch (e: Exception) { }
            isLoadingArticles = false
        }
    }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            isLoadingBookmarks = true
            try {
                val db = FirebaseFirestore.getInstance()
                val bookmarkDocs = db.collection("users").document(uid).collection("bookmarks")
                    .orderBy("timestamp", Query.Direction.DESCENDING).limit(20).get().await()
                val articleIds = bookmarkDocs.documents.mapNotNull { it.getString("articleId") }
                if (articleIds.isNotEmpty()) {
                    val fetched = mutableListOf<ArticleItem>()
                    articleIds.chunked(10).forEach { chunk ->
                        try {
                            val articleDocs = db.collection("articles").whereIn("__name__", chunk).get().await()
                            fetched += articleDocs.documents.mapNotNull { doc ->
                                val data = doc.data ?: return@mapNotNull null
                                ArticleItem(id = doc.id, title = data["title"] as? String ?: "", snippet = data["snippet"] as? String ?: "", fullContent = data["fullContent"] as? String ?: "", imageUrl = data["headerImageUrl"] as? String)
                            }.filter { it.title.isNotBlank() }
                        } catch (e: Exception) { }
                    }
                    bookmarkedArticles = fetched
                }
            } catch (e: Exception) { }
            isLoadingBookmarks = false
        }
    }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            try {
                FirebaseFirestore.getInstance().collection("users").document(uid).collection("activity")
                    .orderBy("timestamp", Query.Direction.DESCENDING).limit(30)
                    .addSnapshotListener { snap, _ -> snap?.let { activityViewModel.updateFromFirestore(it.documents) } }
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
        if (!fbGames.isNullOrEmpty()) fbGames.map { Game(
            name = it["name"] as? String ?: "",
            coverUrl = (it["coverUrl"] as? String)?.ifBlank { null },
            platform = it["platform"] as? String
        )}.filter { it.name.isNotBlank() }
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

    // Checkpoint streak from Firebase
    var checkpointStreak by remember { mutableStateOf(0) }
    var totalCheckpoints by remember { mutableStateOf(0) }
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            try {
                val snap = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(uid).get().await()
                checkpointStreak = (snap.getLong("currentStreak") ?: 0L).toInt()
                totalCheckpoints = (snap.getLong("totalCheckpoints") ?: 0L).toInt()
            } catch (ignored: Exception) {}
        }
    }
    val profilePicSize = 110.dp
    val bannerHeight = 220.dp
    val genreScores = remember(displayGames) { detectGenresFromGames(displayGames) }

    val neonT = rememberInfiniteTransition(label = "neonGlobal")
    val neonAlpha by rememberGlowRange(0.4f, 1f)
    val ringRotation by neonT.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "ringRot")
    val starDrift by neonT.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart), label = "starDrift")

    Box(modifier = modifier.fillMaxSize().background(ComicGlassBg)) {

        // ── Micro halftone dot background (Option A) ──────────────────────────
        HalftoneDots(Modifier.fillMaxSize(), spacing = 8.dp, dotRadius = 1.2.dp, color = Color(0xFF1B4332).copy(alpha = 0.13f))

        // ── Floating pixel icons + drifting star field ────────────────────────
        val stars = remember { (0..60).map { Triple((Math.random() * 400).toFloat(), (Math.random() * 1200).toFloat(), (Math.random() * 0.6f + 0.2f).toFloat()) } }
        val pixelIconColors = remember { listOf(
            CGreen, CAcPurple, CGreenMint, CAcRed, Color(0xFFFFFFFF)
        ) }
        val floatingIcons = remember {
            (0..19).map { i ->
                PixelIconDef(
                    xRatio  = (Math.random() * 0.86f + 0.07f).toFloat(),
                    yRatio  = Math.random().toFloat(),
                    speed   = (Math.random() * 0.28f + 0.09f).toFloat(),
                    shapeIdx = i % allPixelShapes.size,
                    cellDp  = (Math.random() * 2.0f + 1.5f).toFloat(),  // 1.5–3.5 dp per pixel cell
                    alpha   = (Math.random() * 0.07f + 0.03f).toFloat(), // 3–10% opacity
                    colorIdx = i % pixelIconColors.size
                )
            }
        }
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            // Floating retro pixel icons — drift upward slowly
            floatingIcons.forEach { icon ->
                val driftedY = ((icon.yRatio - starDrift * icon.speed * 0.45f) + 2f) % 1f
                val startX = icon.xRatio * size.width
                val startY = driftedY * size.height
                val cellPx = icon.cellDp.dp.toPx()
                val iconColor = pixelIconColors[icon.colorIdx].copy(alpha = icon.alpha * neonAlpha)
                allPixelShapes[icon.shapeIdx].forEach { (row, col) ->
                    drawRect(
                        color = iconColor,
                        topLeft = Offset(startX + col * cellPx, startY + row * cellPx),
                        size = Size(cellPx - 0.5f, cellPx - 0.5f)
                    )
                }
            }
            // Drifting pixel stars (fine sparkle layer)
            stars.forEach { star ->
                val x = star.first; val y = star.second; val brightness = star.third
                val driftedY = ((y + starDrift * 300f) % size.height)
                drawCircle(color = Color.White.copy(alpha = brightness * neonAlpha * 0.5f),
                    radius = if (brightness > 0.7f) 2f else 1.2f,
                    center = Offset(x % size.width, driftedY))
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {

            item {
                Box(modifier = Modifier.fillMaxWidth().height(bannerHeight).then(if (isEditing) Modifier.clickable { bannerPickerLauncher.launch("image/*") } else Modifier)) {
                    val bannerUrl = firebaseProfile?.bannerUrl
                    if (!bannerUrl.isNullOrBlank()) {
                        val kbT = rememberInfiniteTransition(label = "bannerKB")
                        val bannerScale by kbT.animateFloat(initialValue = 1f, targetValue = 1.07f, animationSpec = infiniteRepeatable(keyframes { durationMillis = 16000; 1f at 0; 1.07f at 8000; 1f at 16000 }, RepeatMode.Restart), label = "bannerScale")
                        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(0.dp))) {
                            AsyncImage(model = bannerUrl, contentDescription = "Banner", modifier = Modifier.halftoneReveal(bannerUrl).fillMaxSize().scale(bannerScale), contentScale = ContentScale.Crop)
                        }
                    } else {
                        val gradT = rememberInfiniteTransition(label = "bannerGrad")
                        val gradOffset by gradT.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse), label = "gradOffset")
                        val scanT = rememberInfiniteTransition(label = "scan")
                        val scanY by scanT.animateFloat(initialValue = -220f, targetValue = 220f, animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Restart), label = "scanLine")
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(CGreen, CGreenMint.copy(alpha = 0.55f + gradOffset * 0.3f), ComicGlassBg))))
                        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly) { repeat(12) { Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(CGreen.copy(alpha = 0.04f))) } }
                        Box(modifier = Modifier.fillMaxSize().offset(y = scanY.dp)) { Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Brush.horizontalGradient(colors = listOf(Color.Transparent, CGreen.copy(alpha = 0.3f), CGreen.copy(alpha = 0.6f), CGreen.copy(alpha = 0.3f), Color.Transparent)))) }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🕹️", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("RETROHUB", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.12f), fontSize = 52.sp, letterSpacing = 8.sp)
                            }
                        }
                    }
                    // Bottom fade gradient
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.55f)))))
                    // CRT scanlines overlay (subtle)
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        var scanY = 0f
                        while (scanY < size.height) {
                            drawLine(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.06f), androidx.compose.ui.geometry.Offset(0f, scanY), androidx.compose.ui.geometry.Offset(size.width, scanY), strokeWidth = 1.5f)
                            scanY += 3f
                        }
                    }
                    if (isUploadingBanner) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { ThreeDotsAnimation(); Text("Uploading banner...", fontFamily = NunitoFontFamily, color = CGreen, fontSize = 13.sp) }
                        }
                    } else if (isEditing) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(CGreen).border(2.dp, ScrapbookBorder, RoundedCornerShape(20.dp)).padding(horizontal = 14.dp, vertical = 8.dp)) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("TAP TO CHANGE BANNER", fontFamily = BangersFontFamily, color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                    // Logout button – top-start of banner
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(12.dp).size(34.dp).clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .border(1.dp, CAcRed.copy(alpha = 0.5f), CircleShape)
                        .clickable { authViewModel.signOut() },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Logout, contentDescription = "Sign Out", tint = CAcRed.copy(alpha = 0.85f), modifier = Modifier.size(16.dp)) }
                    val lvlScale by rememberGlowRange(1f, 1.04f)
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                        // Pulsing outer glow
                        Box(modifier = Modifier.matchParentSize().offset(x = 7.dp, y = 7.dp)
                            .background(CGreen.copy(alpha = neonAlpha * 0.28f), RoundedCornerShape(20.dp)))
                        // Hard green shadow
                        Box(modifier = Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp)
                            .clip(RoundedCornerShape(20.dp)).background(CGreen))
                        // Badge
                        Box(modifier = Modifier.scale(lvlScale)
                            // Long-press the level badge to replay the LEVEL UP celebration
                            .pointerInput(currentLevel.level) {
                                detectTapGestures(onLongPress = { LevelUpBus.show(currentLevel) })
                            }
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.92f))
                            .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)) {
                            HalftoneDots(Modifier.matchParentSize(), spacing = 6.dp, dotRadius = 1.1.dp, color = Color.Black.copy(alpha = 0.10f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(currentLevel.emoji, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("LVL ${currentLevel.level} · ${currentLevel.title}", fontFamily = BangersFontFamily, color = CGreen, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            item {
                Box(modifier = Modifier.fillMaxWidth().offset(y = (-profilePicSize / 2)), contentAlignment = Alignment.TopCenter) {
                    val ringAlpha by rememberGlowRange(0.3f, 1f)
                    val ringScale by rememberGlowRange(1f, 1.06f)
                    // Outer glow halo
                    Box(modifier = Modifier.size(profilePicSize + 32.dp).scale(ringScale)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(CGreen.copy(alpha = ringAlpha * 0.3f), Color.Transparent))))
                    // Rotating yellow glow ring
                    Box(modifier = Modifier.size(profilePicSize + 16.dp)
                        .graphicsLayer { rotationZ = ringRotation }
                        .clip(CircleShape)
                        .background(Brush.sweepGradient(listOf(
                            CGreen, CGreen.copy(alpha = 0.25f), CGreenDeep,
                            CGreen.copy(alpha = 0.5f), CGreen, CGreenDeep, CGreen
                        ))))
                    // Cream separator ring
                    Box(modifier = Modifier.size(profilePicSize + 8.dp).clip(CircleShape).background(ComicGlassBg))
                    // Star sparkle particles around ring
                    val sparkleT = rememberInfiniteTransition(label = "sparkle")
                    val sparklePhase by sparkleT.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Restart), label = "sparklePhase")
                    val sparkleAlpha by sparkleT.animateFloat(initialValue = 0.25f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(700, easing = EaseInOut), RepeatMode.Reverse), label = "sparkleAlpha")
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(profilePicSize + 44.dp)) {
                        val cx = size.width / 2f; val cy = size.height / 2f
                        val orbitRadius = (profilePicSize.toPx() / 2f) + 18.dp.toPx()
                        val angles = listOf(0f, 55f, 110f, 180f, 235f, 290f)
                        angles.forEachIndexed { i, base ->
                            val rad = Math.toRadians((base + sparklePhase + i * 4f).toDouble())
                            val sx = cx + (orbitRadius * Math.cos(rad)).toFloat()
                            val sy = cy + (orbitRadius * Math.sin(rad)).toFloat()
                            val a = ((sparkleAlpha + i * 0.12f) * ringAlpha).coerceIn(0f, 1f)
                            val r = if (i % 2 == 0) 4.5.dp.toPx() else 2.5.dp.toPx()
                            drawCircle(color = CGreen.copy(alpha = a), radius = r, center = androidx.compose.ui.geometry.Offset(sx, sy))
                        }
                    }
                    // Inner glow ring
                    Box(modifier = Modifier.size(profilePicSize + 4.dp).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(CGreen.copy(alpha = ringAlpha * 0.4f), Color.Transparent))))
                    Box(modifier = Modifier.size(profilePicSize).clip(CircleShape).background(ComicGlassBg).then(if (isEditing) Modifier.clickable { profilePickerLauncher.launch("image/*") } else Modifier), contentAlignment = Alignment.Center) {
                        if (isUploadingProfile) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) { ThreeDotsAnimation(color = CGreenDeep, dotSize = 7.dp) }
                        } else if (!displayProfilePicUrl.isNullOrBlank()) {
                            AsyncImage(model = displayProfilePicUrl, contentDescription = "Profile picture", contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(displayProfilePicUrl).fillMaxSize().clip(CircleShape))
                            if (isEditing) { Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) { Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) } }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = ScrapbookDark.copy(alpha = 0.4f), modifier = Modifier.size(40.dp))
                                if (isEditing) Text("TAP", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.55f), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth().offset(y = (-profilePicSize / 2)).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    uploadMessage?.let { message ->
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(CGreen.copy(alpha = 0.15f)).border(2.dp, CGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                            Text(message, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark, fontSize = 13.sp, textAlign = TextAlign.Center)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    val shimmerT = rememberInfiniteTransition(label = "nameShimmer")
                    val shimmerX by shimmerT.animateFloat(initialValue = -400f, targetValue = 800f, animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "nameShimmerX")
                    Box {
                        Text(displayUsername.uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 34.sp, letterSpacing = 2.sp, textAlign = TextAlign.Center)
                        Text(displayUsername.uppercase(), fontFamily = BangersFontFamily, fontSize = 34.sp, letterSpacing = 2.sp, textAlign = TextAlign.Center, style = TextStyle(brush = Brush.linearGradient(colors = listOf(Color.Transparent, CGreen.copy(alpha = 0.7f), Color.Transparent), start = androidx.compose.ui.geometry.Offset(shimmerX - 150f, 0f), end = androidx.compose.ui.geometry.Offset(shimmerX + 150f, 0f))))
                    }
                    if (displayHandle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        val handleGlowAlpha by rememberGlowPhase(0.45f)
                        Box {
                            // Outer glow
                            Box(modifier = Modifier.matchParentSize().offset(x = 5.dp, y = 5.dp)
                                .clip(RoundedCornerShape(10.dp)).background(CGreen.copy(alpha = handleGlowAlpha * 0.3f)))
                            // Hard shadow
                            Box(modifier = Modifier.matchParentSize().offset(x = 3.dp, y = 3.dp)
                                .clip(RoundedCornerShape(10.dp)).background(CGreen))
                            // Glass label with dots + stamp tilt
                            Box(modifier = Modifier
                                .graphicsLayer { rotationZ = -0.8f }
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.92f))
                                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(10.dp))
                                .padding(horizontal = 11.dp, vertical = 4.dp)
                            ) {
                                HalftoneDots(Modifier.matchParentSize(), spacing = 6.dp, dotRadius = 1.2.dp, color = Color.Black.copy(alpha = 0.12f))
                                Text(displayHandle, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, letterSpacing = 1.sp)
                            }
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
                        item { AnimatedStatCard(formatCount(displayFollowersCount), "FOLLOWERS", neonAlpha) { showFollowersList = true } }
                        item { AnimatedStatCard(formatCount(displayFollowingCount), "FOLLOWING", neonAlpha) { showFollowingList = true } }
                        item { AnimatedStatCard("${achievementsState.articleCount}", "ARTICLES", neonAlpha) { selectedTab = 1 } }
                        item { AnimatedStatCard("${achievementsState.bookmarkCount}", "BOOKMARKS", neonAlpha) { selectedTab = 2 } }
                        item { AnimatedStatCard("${achievementsState.xp}", "TOTAL XP", neonAlpha) { } }
                        if (checkpointStreak > 0) {
                            item { StreakStatCard(checkpointStreak, totalCheckpoints, neonAlpha) }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(14.dp)).padding(4.dp)) {
                        Row {
                            tabs.forEachIndexed { index, title ->
                                val isSelected = selectedTab == index
                                var tabPressed by remember { mutableStateOf(false) }
                                val tabScale by animateFloatAsState(targetValue = if (tabPressed) 0.94f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "tab_$index")
                                Box(modifier = Modifier.weight(1f).scale(tabScale).clip(RoundedCornerShape(11.dp)).background(if (isSelected) CGreen else Color.Transparent).clickable { tabPressed = true; selectedTab = index }.padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
                                    Text(title, fontFamily = BangersFontFamily, fontSize = 15.sp, letterSpacing = 0.5.sp, color = if (isSelected) ScrapbookDark else ScrapbookDark.copy(alpha = 0.55f))
                                }
                                LaunchedEffect(tabPressed) { if (tabPressed) { delay(150); tabPressed = false } }
                            }
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    // 💾 Save Room — your 4 closest friends hanging out in a cozy pixel room
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        NeonSectionHeader("SAVE ROOM", "💾", neonAlpha)
                        Spacer(modifier = Modifier.height(8.dp))
                        SaveRoomSection(
                            ownerUid = currentUser?.uid ?: "",
                            isOwner = true,
                            authViewModel = authViewModel,
                            achievementsViewModel = achievementsViewModel
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        NeonSectionHeader("ACHIEVEMENTS", "🏆", neonAlpha)
                    }
                    item { BadgeShelf(badges = achievementsState.badges) }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        NeonSectionHeader("GAME COLLECTION", "📚", neonAlpha)
                        Spacer(modifier = Modifier.height(8.dp))
                        CollectionStatsCard()
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        val editBtnGlowAlpha by rememberGlowPhase(0.4f)
                        var editBtnPressed by remember { mutableStateOf(false) }
                        val editBtnPressAnim by animateFloatAsState(
                            targetValue = if (editBtnPressed) 4f else 0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "editBtnPressAnim"
                        )
                        val editBtnShadowOff by animateFloatAsState(
                            targetValue = if (editBtnPressed) 0f else 4f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "editBtnShadowOff"
                        )
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Box(modifier = Modifier.matchParentSize().offset(x = 7.dp, y = 7.dp).clip(RoundedCornerShape(10.dp))
                                .background(CGreen.copy(alpha = if (editBtnPressed) 0f else editBtnGlowAlpha * 0.22f)))
                            Box(modifier = Modifier.matchParentSize()
                                .offset(x = editBtnShadowOff.dp, y = editBtnShadowOff.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CGreen.copy(alpha = if (editBtnPressed) 0.3f else 0.9f)))
                            Box(modifier = Modifier.fillMaxWidth()
                                .offset(x = editBtnPressAnim.dp, y = editBtnPressAnim.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.92f))
                                .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(10.dp))
                                .clickable { editBtnPressed = true; isEditing = true }
                                .padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                                // Dot halftone texture
                                HalftoneDots(Modifier.matchParentSize(), spacing = 6.dp, dotRadius = 1.2.dp, color = Color.Black.copy(alpha = 0.12f))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(16.dp))
                                    Text("EDIT PROFILE", fontFamily = BangersFontFamily, fontSize = 18.sp, color = ScrapbookDark, letterSpacing = 1.sp)
                                }
                                ComicShimmer(Modifier.matchParentSize(), cornerRadius = 10.dp)
                            }
                        }
                        LaunchedEffect(editBtnPressed) { if (editBtnPressed) { delay(150); editBtnPressed = false } }
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    if (isEditing) {
                        item {
                            var expandedSections by remember { mutableStateOf(setOf("BASIC")) }
                            var hasUnsavedChanges by remember { mutableStateOf(false) }
                            var showUnsavedDialog by remember { mutableStateOf(false) }
                            LaunchedEffect(editUsername, editBio, editHandle, editLocation, editWebsite, editPsn, editXbox, editSteam, editNintendo, editTwitch, editYoutube, editHabbo, editHabboRegion) { hasUnsavedChanges = true }
                            val completionFields = listOf(editUsername.isNotBlank(), editBio.isNotBlank(), editHandle.isNotBlank(), editLocation.isNotBlank(), editWebsite.isNotBlank(), editPsn.isNotBlank() || editXbox.isNotBlank() || editSteam.isNotBlank() || editNintendo.isNotBlank(), editTwitch.isNotBlank() || editYoutube.isNotBlank(), editHabbo.isNotBlank())
                            val completionPct = (completionFields.count { it } * 100f / completionFields.size).toInt()
                            val editNeonAlpha by rememberGlowRange(0.4f, 1f)

                            if (showUnsavedDialog) {
                                AlertDialog(modifier = Modifier.comicPop(), onDismissRequest = { showUnsavedDialog = false }, containerColor = ComicGlassBg, titleContentColor = ScrapbookDark, textContentColor = ScrapbookDark,
                                    title = { Text("⚠️ UNSAVED CHANGES", fontFamily = BangersFontFamily, fontSize = 20.sp) },
                                    text = { Text("You have unsaved changes. Save before leaving?", fontFamily = NunitoFontFamily, fontSize = 14.sp) },
                                    confirmButton = {
                                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(CGreen).clickable {
                                            showUnsavedDialog = false
                                            firebaseProfile?.let { authViewModel.updateUserProfile(it.copy(username = editUsername, bio = editBio, userHandle = editHandle, location = editLocation, website = editWebsite, psnUsername = editPsn, xboxUsername = editXbox, steamUsername = editSteam, nintendoUsername = editNintendo, twitchUsername = editTwitch, youtubeUsername = editYoutube, habboUsername = editHabbo, habboRegion = editHabboRegion)) }
                                            isEditing = false; achievementsViewModel.fetchAchievements()
                                        }.padding(horizontal = 16.dp, vertical = 10.dp)) { Text("SAVE & EXIT", fontFamily = BangersFontFamily, color = Color.White, fontSize = 14.sp) }
                                    },
                                    dismissButton = {
                                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(CAcRed.copy(alpha = 0.15f)).border(1.dp, CAcRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).clickable { showUnsavedDialog = false; isEditing = false }.padding(horizontal = 16.dp, vertical = 10.dp)) { Text("DISCARD", fontFamily = BangersFontFamily, color = CAcRed, fontSize = 14.sp) }
                                    }
                                )
                            }

                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier.matchParentSize()
                                            .offset(x = 4.dp, y = 4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ScrapbookDark.copy(alpha = 0.12f))
                                    )
                                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookDark, RoundedCornerShape(12.dp)).padding(14.dp)) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("PROFILE COMPLETION", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 11.sp, letterSpacing = 1.sp)
                                            Text("$completionPct%", fontFamily = BangersFontFamily, color = when { completionPct >= 80 -> CGreen; completionPct >= 50 -> CGreen; else -> CAcRed }, fontSize = 18.sp)
                                        }
                                        val animatedCompletion by animateFloatAsState(targetValue = completionPct / 100f, animationSpec = tween(800, easing = LinearOutSlowInEasing), label = "completionAnim")
                                        Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(ScrapbookDark.copy(alpha = 0.1f))) {
                                            Box(modifier = Modifier.fillMaxWidth(animatedCompletion).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(Brush.horizontalGradient(colors = when { completionPct >= 80 -> listOf(CGreen, CGreenMint); completionPct >= 50 -> listOf(CGreen, CGreenDeep); else -> listOf(CAcRed, CAcRed.copy(alpha = 0.7f)) })))
                                        }
                                    }
                                    ComicShimmer(Modifier.matchParentSize(), cornerRadius = 12.dp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                AnimatedVisibility(visible = hasUnsavedChanges, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(CGreen.copy(alpha = 0.08f)).border(1.dp, CGreen.copy(alpha = editNeonAlpha * 0.6f), RoundedCornerShape(8.dp)).padding(horizontal = 14.dp, vertical = 8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(CGreen.copy(alpha = editNeonAlpha)))
                                            Text("CHANGES UNSAVED", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 12.sp, letterSpacing = 1.sp)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                                    val sections = listOf<Pair<String, Color>>(
                                        "BASIC" to CGreen,
                                        "PLATFORMS" to CAcBlue,
                                        "STREAMING" to CAcPurple,
                                        "GAMES" to CAcPurple,
                                        "SOUNDTRACKS" to CAcRed,
                                        "HABBO" to CGreenDeep
                                    )
                                    items(sections) { (key, color) ->
                                        val isActive = expandedSections.contains(key)
                                        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (isActive) color.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.46f)).border(1.5.dp, if (isActive) color.copy(alpha = 0.8f) else ScrapbookDark.copy(alpha = 0.3f), RoundedCornerShape(20.dp)).clickable { expandedSections = if (isActive) expandedSections - key else expandedSections + key }.padding(horizontal = 14.dp, vertical = 7.dp)) {
                                            Text(key, fontFamily = BangersFontFamily, color = if (isActive) color else ScrapbookDark.copy(alpha = 0.55f), fontSize = 11.sp, letterSpacing = 1.sp)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))

                                val basicFilled = listOf(editUsername.isNotBlank(), editHandle.isNotBlank(), editBio.isNotBlank(), editLocation.isNotBlank(), editWebsite.isNotBlank())
                                AccordionSection(emoji = "👤", title = "BASIC INFO", accentColor = CGreen, isExpanded = expandedSections.contains("BASIC"), completionFraction = basicFilled.count { !it }.toFloat() / basicFilled.size, onToggle = { expandedSections = if (expandedSections.contains("BASIC")) expandedSections - "BASIC" else expandedSections + "BASIC" }) {
                                    RetroTerminalInput(value = editUsername, onValueChange = { editUsername = it }, label = "USERNAME", maxChars = 20)
                                    RetroTerminalInput(value = editHandle, onValueChange = { v -> editHandle = if (v.isNotBlank() && !v.startsWith("@")) "@$v" else v }, label = "HANDLE", maxChars = 20)
                                    RetroTerminalInput(value = editBio, onValueChange = { editBio = it }, label = "BIO", maxChars = 150, singleLine = false)
                                    RetroTerminalInput(value = editLocation, onValueChange = { editLocation = it }, label = "LOCATION")
                                    RetroTerminalInput(value = editWebsite, onValueChange = { v -> editWebsite = if (v.isNotBlank() && !v.startsWith("http") && v.contains(".")) "https://$v" else v }, label = "WEBSITE")
                                }
                                Spacer(modifier = Modifier.height(10.dp))

                                val platformFilled = listOf(editPsn.isNotBlank(), editXbox.isNotBlank(), editSteam.isNotBlank(), editNintendo.isNotBlank())
                                AccordionSection(emoji = "🎮", title = "GAMING PLATFORMS", accentColor = CAcBlue, isExpanded = expandedSections.contains("PLATFORMS"), completionFraction = platformFilled.count { !it }.toFloat() / platformFilled.size, onToggle = { expandedSections = if (expandedSections.contains("PLATFORMS")) expandedSections - "PLATFORMS" else expandedSections + "PLATFORMS" }) {
                                    PlatformInputWithVerify(editPsn, { editPsn = it }, gamingPlatforms[0])
                                    PlatformInputWithVerify(editXbox, { editXbox = it }, gamingPlatforms[1])
                                    PlatformInputWithVerify(editSteam, { editSteam = it }, gamingPlatforms[2])
                                    PlatformInputWithVerify(editNintendo, { editNintendo = it }, gamingPlatforms[3])
                                }
                                Spacer(modifier = Modifier.height(10.dp))

                                val streamFilled = listOf(editTwitch.isNotBlank(), editYoutube.isNotBlank())
                                AccordionSection(emoji = "📡", title = "STREAMING", accentColor = CAcPurple, isExpanded = expandedSections.contains("STREAMING"), completionFraction = streamFilled.count { !it }.toFloat() / streamFilled.size, onToggle = { expandedSections = if (expandedSections.contains("STREAMING")) expandedSections - "STREAMING" else expandedSections + "STREAMING" }) {
                                    RetroTerminalInput(value = editTwitch, onValueChange = { editTwitch = it }, label = "TWITCH USERNAME")
                                    RetroTerminalInput(value = editYoutube, onValueChange = { editYoutube = it }, label = "YOUTUBE USERNAME")
                                }
                                Spacer(modifier = Modifier.height(10.dp))

                                // TOP GAMES EDITOR
                                AccordionSection(emoji = "🎮", title = "TOP GAMES", accentColor = CAcPurple, isExpanded = expandedSections.contains("GAMES"), completionFraction = 0f, onToggle = { expandedSections = if (expandedSections.contains("GAMES")) expandedSections - "GAMES" else expandedSections + "GAMES" }) {
                                    TopGamesEditorContent(
                                        currentGames = firebaseProfile?.topGames ?: emptyList(),
                                        onGamesChanged = { newGames ->
                                            firebaseProfile?.let { authViewModel.updateUserProfile(it.copy(topGames = newGames)) }
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))

                                // TOP SOUNDTRACKS EDITOR
                                AccordionSection(emoji = "🎵", title = "TOP SOUNDTRACKS", accentColor = CAcRed, isExpanded = expandedSections.contains("SOUNDTRACKS"), completionFraction = 0f, onToggle = { expandedSections = if (expandedSections.contains("SOUNDTRACKS")) expandedSections - "SOUNDTRACKS" else expandedSections + "SOUNDTRACKS" }) {
                                    TopSoundtracksEditorContent(
                                        currentSoundtracks = firebaseProfile?.topSoundtracks ?: emptyList(),
                                        onSoundtracksChanged = { newTracks ->
                                            firebaseProfile?.let { authViewModel.updateUserProfile(it.copy(topSoundtracks = newTracks)) }
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))

                                AccordionSection(emoji = "🏨", title = "HABBO HOTEL", accentColor = CGreenDeep, isExpanded = expandedSections.contains("HABBO"), completionFraction = if (editHabbo.isBlank()) 1f else 0f, onToggle = { expandedSections = if (expandedSections.contains("HABBO")) expandedSections - "HABBO" else expandedSections + "HABBO" }) {
                                    Text("▶ SELECT YOUR HOTEL", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 11.sp, letterSpacing = 1.sp)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                                        items(habboRegions) { (label, domain, _) ->
                                            val isSelected = editHabboRegion == domain
                                            var regionPressed by remember { mutableStateOf(false) }
                                            val regionScale by animateFloatAsState(targetValue = if (regionPressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "regionScale")
                                            Box(modifier = Modifier.scale(regionScale).clip(RoundedCornerShape(20.dp)).background(if (isSelected) CGreenDeep.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.46f)).border(width = if (isSelected) 2.dp else 1.dp, color = if (isSelected) CGreenMint else ScrapbookDark.copy(alpha = 0.3f), shape = RoundedCornerShape(20.dp)).clickable { regionPressed = true; editHabboRegion = domain }.padding(horizontal = 12.dp, vertical = 7.dp)) {
                                                Text(text = label, fontFamily = BangersFontFamily, color = if (isSelected) CGreenDeep else ScrapbookDark.copy(alpha = 0.55f), fontSize = 12.sp)
                                            }
                                            LaunchedEffect(regionPressed) { if (regionPressed) { delay(150); regionPressed = false } }
                                        }
                                    }
                                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(CGreenDeep.copy(alpha = 0.1f)).border(1.dp, CGreenMint.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("🌐", fontSize = 14.sp); Text("Hotel: www.$editHabboRegion", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark.copy(alpha = 0.7f), fontSize = 13.sp) }
                                    }
                                    RetroTerminalInput(value = editHabbo, onValueChange = { editHabbo = it }, label = "HABBO USERNAME")
                                    if (editHabbo.isNotBlank()) {
                                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ComicGlassBg).border(1.5.dp, CGreenMint.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                            AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(habboAvatarUrl(editHabbo, editHabboRegion)).crossfade(true).diskCachePolicy(CachePolicy.DISABLED).memoryCachePolicy(CachePolicy.DISABLED).build(), contentDescription = "Habbo Avatar Preview", contentScale = ContentScale.Fit, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.3f)), error = rememberVectorPainter(image = Icons.Filled.SmartToy), fallback = rememberVectorPainter(image = Icons.Filled.SmartToy))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(CGreenMint.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 3.dp)) { Text("LIVE PREVIEW", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 10.sp) }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(editHabbo, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                                                Text("www.$editHabboRegion", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.55f), fontSize = 11.sp)
                                                Text("🤖 This will be your talking robot", fontFamily = NunitoFontFamily, color = CGreenDeep, fontSize = 10.sp)
                                            }
                                        }
                                    } else {
                                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.92f)).border(1.dp, ScrapbookDark.copy(alpha = 0.3f), RoundedCornerShape(10.dp)).padding(16.dp), contentAlignment = Alignment.Center) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Text("🏨", fontSize = 28.sp)
                                                Column { Text("ADD YOUR HABBO USERNAME", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 14.sp); Text("Pick your hotel above first!", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.55f), fontSize = 11.sp) }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                                val saveGlowAlpha by rememberGlowPhase(0.4f)
                                var savePressed by remember { mutableStateOf(false) }
                                val savePressAnim by animateFloatAsState(
                                    targetValue = if (savePressed) 4f else 0f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    label = "savePressAnim"
                                )
                                val saveShadowOff by animateFloatAsState(
                                    targetValue = if (savePressed) 0f else 4f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    label = "saveShadowOff"
                                )
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.matchParentSize().offset(x = 7.dp, y = 7.dp).clip(RoundedCornerShape(12.dp))
                                        .background(CGreen.copy(alpha = if (savePressed) 0f else saveGlowAlpha * 0.22f)))
                                    Box(modifier = Modifier.matchParentSize()
                                        .offset(x = saveShadowOff.dp, y = saveShadowOff.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CGreen.copy(alpha = if (savePressed) 0.3f else 0.9f)))
                                    Box(modifier = Modifier.fillMaxWidth()
                                        .offset(x = savePressAnim.dp, y = savePressAnim.dp)
                                        .clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(12.dp)).clickable {
                                            savePressed = true
                                            firebaseProfile?.let { authViewModel.updateUserProfile(it.copy(username = editUsername, bio = editBio, userHandle = editHandle, location = editLocation, website = editWebsite, psnUsername = editPsn, xboxUsername = editXbox, steamUsername = editSteam, nintendoUsername = editNintendo, twitchUsername = editTwitch, youtubeUsername = editYoutube, habboUsername = editHabbo, habboRegion = editHabboRegion)) }
                                            currentUser?.uid?.let { uid -> FirebaseFirestore.getInstance().collection("users").document(uid).set(mapOf("profileCompletion" to completionPct), com.google.firebase.firestore.SetOptions.merge()) }
                                            hasUnsavedChanges = false; isEditing = false; achievementsViewModel.fetchAchievements()
                                        }.padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                        // Dot halftone texture
                                        HalftoneDots(Modifier.matchParentSize(), spacing = 6.dp, dotRadius = 1.2.dp, color = Color.Black.copy(alpha = 0.12f))
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(20.dp))
                                            Text("SAVE PROFILE", fontFamily = BangersFontFamily, fontSize = 20.sp, color = ScrapbookDark, letterSpacing = 1.sp)
                                        }
                                        ComicShimmer(Modifier.matchParentSize(), cornerRadius = 12.dp)
                                    }
                                }
                                LaunchedEffect(savePressed) { if (savePressed) { delay(200); savePressed = false } }
                                Spacer(modifier = Modifier.height(10.dp))
                                var cancelPressed by remember { mutableStateOf(false) }
                                val cancelPressAnim by animateFloatAsState(targetValue = if (cancelPressed) 3f else 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "cancelPressAnim")
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.matchParentSize().offset(x = (if (cancelPressed) 0f else 3f).dp, y = (if (cancelPressed) 0f else 3f).dp).clip(RoundedCornerShape(12.dp)).background(CAcRed.copy(alpha = if (cancelPressed) 0.15f else 0.5f)))
                                    Box(modifier = Modifier.fillMaxWidth().offset(x = cancelPressAnim.dp, y = cancelPressAnim.dp).clip(RoundedCornerShape(12.dp)).background(Color.Transparent).border(2.dp, CAcRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).clickable { cancelPressed = true; if (hasUnsavedChanges) showUnsavedDialog = true else isEditing = false }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                                        Text("CANCEL", fontFamily = BangersFontFamily, fontSize = 16.sp, color = CAcRed.copy(alpha = 0.8f), letterSpacing = 1.sp)
                                    }
                                }
                                LaunchedEffect(cancelPressed) { if (cancelPressed) { delay(150); cancelPressed = false } }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                    if (!isEditing) {
                        item {
                            NeonSectionHeader("ABOUT ME", "👤", neonAlpha)
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                BioCard(bioText = displayBio, profilePicUrl = displayProfilePicUrl, habboUsername = displayHabbo, habboRegion = displayHabboRegion)
                                if (displayLocation.isNotBlank() || displayWebsite.isNotBlank()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        if (displayLocation.isNotBlank()) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = ScrapbookTextMuted, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(displayLocation, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        if (displayWebsite.isNotBlank()) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).clickable {
                                                val url = if (displayWebsite.startsWith("http")) displayWebsite else "https://$displayWebsite"
                                                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) { }
                                            }) {
                                                Icon(Icons.Filled.Language, contentDescription = null, tint = ScrapbookTextMuted, modifier = Modifier.size(14.dp))
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
                                if (displayHabbo.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val habboNeonAlpha by rememberGlowRange(0.4f, 1f)
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
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Box(
                                            modifier = Modifier.matchParentSize()
                                                .offset(x = 4.dp, y = 4.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(ScrapbookDark.copy(alpha = 0.12f))
                                        )
                                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                            .background(Brush.verticalGradient(listOf(ComicGlassBg, ComicGlassBg, CGreenMint)))
                                            .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(16.dp))) {
                                        // Subtle warm pattern overlay
                                        Row(modifier = Modifier.fillMaxWidth().height(200.dp), horizontalArrangement = Arrangement.SpaceEvenly) { repeat(12) { Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(CGreen.copy(alpha = 0.08f))) } }
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(CGreen.copy(alpha = flickerAlpha * 0.15f)).border(1.5.dp, CGreen.copy(alpha = flickerAlpha), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) { Text("🏨 HABBO HOTEL", fontFamily = BangersFontFamily, color = CGreenDeep.copy(alpha = flickerAlpha), fontSize = 14.sp, letterSpacing = 1.sp) }
                                                    Text(regionFlag, fontSize = 18.sp)
                                                }
                                                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(CGreen.copy(alpha = 0.1f)).border(1.dp, CGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).clickable { val url = "https://www.$displayHabboRegion/profile/$displayHabbo"; try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) { } }.padding(horizontal = 10.dp, vertical = 5.dp)) { Text("VISIT ROOM →", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 11.sp) }
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                val floatY by rememberGlowPhase(-5f)
                                                Box(modifier = Modifier.offset(y = (avatarOffsetY + floatY).dp).graphicsLayer { alpha = avatarAlpha }) {
                                                    Box(modifier = Modifier.size(150.dp).offset(y = 10.dp).clip(CircleShape).background(Brush.radialGradient(colors = listOf(CGreen.copy(alpha = habboNeonAlpha * 0.2f), Color.Transparent))))
                                                    Box(modifier = Modifier.size(140.dp).clip(RoundedCornerShape(12.dp)).background(Color.Transparent).clickable { currentPose = "wav" }, contentAlignment = Alignment.BottomCenter) {
                                                        val poseUrl = remember(currentPose, displayHabbo, displayHabboRegion) { "https://www.$displayHabboRegion/habbo-imaging/avatarimage?user=${displayHabbo.trim()}&action=$currentPose&direction=2&head_direction=2&size=l&gesture=sml" }
                                                        AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(poseUrl).crossfade(true).diskCachePolicy(CachePolicy.DISABLED).memoryCachePolicy(CachePolicy.DISABLED).build(), contentDescription = "Habbo Avatar", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize(), error = rememberVectorPainter(image = Icons.Filled.SmartToy), fallback = rememberVectorPainter(image = Icons.Filled.SmartToy))
                                                    }
                                                    LaunchedEffect(currentPose) { if (currentPose == "wav") { delay(2000); currentPose = "std" } }
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(displayHabbo, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 0.5.sp)
                                                    Text("www.$displayHabboRegion", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    AnimatedVisibility(visible = greetingVisible, enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 2 }), exit = fadeOut(tween(300))) {
                                                        Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(CGreen.copy(alpha = 0.18f)).border(1.dp, CGreen.copy(alpha = 0.55f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Text("💬", fontSize = 10.sp); Text(currentGreeting, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark, fontSize = 12.sp) }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        poses.forEach { posePair ->
                                                            val pose = posePair.first; val label = posePair.second
                                                            val isActive = currentPose == pose
                                                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (isActive) CGreen else CGreen.copy(alpha = 0.1f)).border(1.dp, CGreen.copy(alpha = if (isActive) 1f else 0.3f), RoundedCornerShape(8.dp)).clickable { currentPose = pose }.padding(horizontal = 8.dp, vertical = 5.dp)) {
                                                                Text(label, fontFamily = BangersFontFamily, color = if (isActive) ScrapbookDark else CGreenDeep, fontSize = 10.sp)
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(CGreen.copy(alpha = 0.1f)).border(1.dp, CGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) { Text("🤖 Talking robot", fontFamily = NunitoFontFamily, color = CGreenDeep, fontSize = 10.sp) }
                                                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(CAcBlue.copy(alpha = 0.2f)).border(1.dp, CAcBlue.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).clickable { val url = "https://www.$displayHabboRegion/profile/$displayHabbo"; try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) { } }.padding(horizontal = 8.dp, vertical = 4.dp)) { Text("➕ Add Friend", fontFamily = BangersFontFamily, color = CAcBlue, fontSize = 10.sp) }
                                                    }
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
                            PinnedArticleSection(
                                pinnedArticle = pinnedArticle, isEditing = isEditing, userArticles = userArticles,
                                onPin = { article -> pinnedArticle = article; currentUser?.uid?.let { uid -> FirebaseFirestore.getInstance().collection("users").document(uid).update("pinnedArticleId", article.id) } },
                                onUnpin = { pinnedArticle = null; currentUser?.uid?.let { uid -> FirebaseFirestore.getInstance().collection("users").document(uid).update("pinnedArticleId", "") } }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        RetroEraSection(userId = currentUser?.uid ?: "")
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        BacklogStatsSection(userId = currentUser?.uid ?: "")
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        NeonSectionHeader("GAME SHELF", "📦", neonAlpha)
                        Spacer(modifier = Modifier.height(8.dp))
                        TopGamesSection(games = displayGames, topGenre = genreScores.maxByOrNull { it.score }?.genre ?: "")
                        Spacer(modifier = Modifier.height(24.dp))
                        NeonSectionHeader("MY TOP 3 SOUNDTRACKS", "🎵", neonAlpha)
                        TopSoundtracksSection(soundtracks = displaySoundtracks, currentUserId = currentUser?.uid ?: "")
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        NeonSectionHeader("GAMING PERSONALITY", "🎯", neonAlpha)
                        Spacer(modifier = Modifier.height(8.dp))
                        RetroRadarChart(genreScores)
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        NeonSectionHeader("ACTIVITY", "⚡", neonAlpha)
                        Spacer(modifier = Modifier.height(8.dp))
                        ActivityStreakSection(displayActivities, firebaseProfile?.createdAt ?: 0L)
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        NeonSectionHeader("RECENT ACTIVITY", "📋", neonAlpha)
                        RealActivitySection(displayActivities, isLoadingActivity, displayUsername, displayProfilePicUrl) { selectedActivityArticle = it }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                1 -> {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        ProfileArticlesTab(articles = userArticles, isLoading = isLoadingArticles, username = displayUsername)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                2 -> {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        ProfileBookmarksTab(bookmarks = bookmarkedArticles, isLoading = isLoadingBookmarks)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        if (showFollowersList) {
            Box(modifier = Modifier.fillMaxSize().background(ComicGlassBg)) {
                FollowListScreen(listType = FollowListType.FOLLOWERS, authViewModel = authViewModel, onBack = { showFollowersList = false })
            }
        }
        if (showFollowingList) {
            Box(modifier = Modifier.fillMaxSize().background(ComicGlassBg)) {
                FollowListScreen(listType = FollowListType.FOLLOWING, authViewModel = authViewModel, onBack = { showFollowingList = false })
            }
        }
        selectedActivityArticle?.let { activityItem ->
            Box(modifier = Modifier.fillMaxSize().background(ComicGlassBg)) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(CGreen, CGreenMint, CGreen))).border(BorderStroke(2.dp, ScrapbookBorder)).padding(top = 40.dp, bottom = 12.dp, start = 8.dp, end = 16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { selectedActivityArticle = null }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Close", tint = ScrapbookDark) }
                            Text("ARTICLE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 24.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.size(48.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ArticleCard(article = ArticleItem(id = activityItem.id, title = activityItem.itemTitle, snippet = activityItem.itemSnippet, fullContent = activityItem.itemSnippet, imageUrl = activityItem.itemImageUrl.ifBlank { null }), gradientColors = articleGradientColorsList[0], initiallyExpanded = true)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
// ─── Articles Tab ─────────────────────────────────────────────────────────────

@Composable
fun ProfileArticlesTab(articles: List<ArticleItem>, isLoading: Boolean, username: String) {
    val neonAlpha by rememberGlowRange(0.4f, 1f)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.width(4.dp).height(28.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(alpha = neonAlpha)))
                Text("✍️", fontSize = 18.sp)
                Text("MY ARTICLES", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.sp)
            }
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookDark).border(1.dp, CGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text("${articles.size}", fontFamily = BangersFontFamily, color = CGreen, fontSize = 14.sp)
            }
        }
        when {
            isLoading -> Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { ThreeDotsAnimation(); Text("Loading articles...", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp) } }
            articles.isEmpty() -> Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp)).padding(32.dp), contentAlignment = Alignment.Center) {
                // Green stripe
                Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                    .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("✍️", fontSize = 40.sp)
                    Text("NO ARTICLES YET", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
                    Text("Head to the Articles section\nto write your first piece!", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
                    Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(ScrapbookDark).border(1.dp, CGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp)).padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Text("📰", fontSize = 14.sp); Text("GO TO ARTICLES", fontFamily = BangersFontFamily, color = CGreen, fontSize = 14.sp) }
                    }
                }
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { articles.forEach { ProfileArticleCard(article = it) } }
        }
    }
}

@Composable
fun ProfileArticleCard(article: ArticleItem) {
    var expanded by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.98f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "articleCardScale")
    Box(modifier = Modifier.fillMaxWidth().scale(cardScale)) {
        Box(modifier = Modifier.fillMaxWidth().offset(x = 3.dp, y = 3.dp).clip(RoundedCornerShape(12.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(12.dp))
            .clickable { pressed = true; expanded = !expanded }) {
            // Green stripe
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
            Column {
                if (!article.imageUrl.isNullOrBlank()) { AsyncImage(model = article.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(article.imageUrl).fillMaxWidth().height(140.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))) }
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Text(article.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, lineHeight = 22.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = ScrapbookTextMuted, modifier = Modifier.size(20.dp).padding(start = 8.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(article.snippet, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, lineHeight = 18.sp, maxLines = if (expanded) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
                    AnimatedVisibility(visible = expanded) {
                        Column { Spacer(modifier = Modifier.height(10.dp)); HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.4f)); Spacer(modifier = Modifier.height(10.dp)); Text(article.fullContent, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.85f), fontSize = 14.sp, lineHeight = 22.sp) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookDark).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text(if (expanded) "COLLAPSE" else "READ MORE", fontFamily = BangersFontFamily, color = CGreen, fontSize = 10.sp, letterSpacing = 0.5.sp)
                    }
                }
            }
            ComicShimmer(Modifier.matchParentSize(), cornerRadius = 12.dp)
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Bookmarks Tab ────────────────────────────────────────────────────────────

@Composable
fun ProfileBookmarksTab(bookmarks: List<ArticleItem>, isLoading: Boolean) {
    val neonAlpha by rememberGlowRange(0.4f, 1f)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.width(4.dp).height(28.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(alpha = neonAlpha)))
                Text("🔖", fontSize = 18.sp)
                Text("MY BOOKMARKS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.sp)
            }
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookDark).border(1.dp, CGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text("${bookmarks.size}", fontFamily = BangersFontFamily, color = CGreen, fontSize = 14.sp)
            }
        }
        when {
            isLoading -> Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { ThreeDotsAnimation(); Text("Loading bookmarks...", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp) } }
            bookmarks.isEmpty() -> Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp)).padding(32.dp), contentAlignment = Alignment.Center) {
                // Green stripe
                Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                    .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🔖", fontSize = 40.sp)
                    Text("NO BOOKMARKS YET", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
                    Text("Bookmark articles from the\nArticles section to save them here!", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
                }
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { bookmarks.forEachIndexed { index, article -> ProfileBookmarkCard(article = article, index = index) } }
        }
    }
}

@Composable
fun ProfileBookmarkCard(article: ArticleItem, index: Int) {
    var expanded by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.98f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "bookmarkCardScale_$index")
    var visible by remember { mutableStateOf(false) }
    val enterOffset by animateFloatAsState(targetValue = if (visible) 0f else 30f, animationSpec = tween(350, delayMillis = index * 60, easing = LinearOutSlowInEasing), label = "bookmarkEnter_$index")
    val enterAlpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(300, delayMillis = index * 60), label = "bookmarkAlpha_$index")
    LaunchedEffect(Unit) { visible = true }
    Box(modifier = Modifier.fillMaxWidth().offset(y = enterOffset.dp).graphicsLayer { alpha = enterAlpha }.scale(cardScale)) {
        Box(modifier = Modifier.fillMaxWidth().offset(x = 3.dp, y = 3.dp).clip(RoundedCornerShape(12.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(12.dp))
            .clickable { pressed = true; expanded = !expanded }) {
            // Green stripe
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(Brush.verticalGradient(colors = listOf(CGreen, CGreenDeep))).clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)))
                Column(modifier = Modifier.weight(1f).padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                            Text("🔖", fontSize = 12.sp)
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(CGreen.copy(alpha = 0.25f)).border(1.dp, CGreen.copy(0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("SAVED", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 9.sp, letterSpacing = 0.5.sp) }
                        }
                        Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = ScrapbookTextMuted, modifier = Modifier.size(18.dp))
                    }
                    if (!article.imageUrl.isNullOrBlank() && expanded) { AsyncImage(model = article.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(article.imageUrl).fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp))); Spacer(modifier = Modifier.height(8.dp)) }
                    Text(article.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, lineHeight = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(article.snippet, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, lineHeight = 17.sp, maxLines = if (expanded) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
                    AnimatedVisibility(visible = expanded) {
                        Column { Spacer(modifier = Modifier.height(8.dp)); HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.4f)); Spacer(modifier = Modifier.height(8.dp)); Text(article.fullContent, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.85f), fontSize = 13.sp, lineHeight = 20.sp) }
                    }
                }
            }
            ComicShimmer(Modifier.matchParentSize(), cornerRadius = 12.dp)
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Other User Profile ───────────────────────────────────────────────────────

@Composable
fun OtherUserProfileScreen(userId: String, onBack: () -> Unit, authViewModel: AuthViewModel = viewModel()) {
    var profile by remember { mutableStateOf<com.google.firebase.firestore.DocumentSnapshot?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isFollowing by remember { mutableStateOf(false) }
    var followersCount by remember { mutableStateOf(0) }
    val currentUser by authViewModel.currentUser.collectAsState()

    LaunchedEffect(userId) {
        try {
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("users").document(userId).get().await()
            profile = doc
            followersCount = (doc.getLong("followersCount") ?: 0).toInt()
            currentUser?.uid?.let { myUid ->
                val followDoc = db.collection("users").document(myUid).collection("following").document(userId).get().await()
                isFollowing = followDoc.exists()
            }
        } catch (e: Exception) { }
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize().background(ComicGlassBg)) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { ThreeDotsAnimation(); Text("Loading profile...", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp) }
            }
        } else {
            val data = profile?.data
            val username = data?.get("username") as? String ?: "Retro Gamer"
            val userHandle = data?.get("userHandle") as? String ?: ""
            val bio = data?.get("bio") as? String ?: ""
            val profilePicUrl = data?.get("profilePictureUrl") as? String
            val bannerUrl = data?.get("bannerUrl") as? String
            val followingCount = (data?.get("followingCount") as? Long)?.toInt() ?: 0
            val location = data?.get("location") as? String ?: ""
            val website = data?.get("website") as? String ?: ""
            val habboUsername = data?.get("habboUsername") as? String ?: ""
            val habboRegion = (data?.get("habboRegion") as? String)?.ifBlank { "habbo.com" } ?: "habbo.com"
            val psnUsername = data?.get("psnUsername") as? String ?: ""
            val xboxUsername = data?.get("xboxUsername") as? String ?: ""
            val steamUsername = data?.get("steamUsername") as? String ?: ""
            val nintendoUsername = data?.get("nintendoUsername") as? String ?: ""
            val twitchUsername = data?.get("twitchUsername") as? String ?: ""
            val youtubeUsername = data?.get("youtubeUsername") as? String ?: ""
            val context = LocalContext.current

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(CGreen, CGreenMint, CGreen))).border(BorderStroke(2.dp, ScrapbookBorder)).padding(top = 16.dp, bottom = 12.dp, start = 8.dp, end = 16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ScrapbookDark) }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(username.uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 24.sp, letterSpacing = 2.sp)
                                if (userHandle.isNotBlank()) Text(userHandle, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 12.sp)
                            }
                        }
                    }
                }
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                        if (!bannerUrl.isNullOrBlank()) { AsyncImage(model = bannerUrl, contentDescription = "Banner", contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(bannerUrl).fillMaxSize()) }
                        else { Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(CGreen, CGreenMint)))); Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("RETROHUB", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.08f), fontSize = 40.sp, letterSpacing = 6.sp) } }
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)))))
                    }
                }
                item {
                    Box(modifier = Modifier.fillMaxWidth().offset(y = (-55).dp), contentAlignment = Alignment.TopCenter) {
                        val ringAlpha by rememberGlowRange(0.3f, 1f)
                        Box(modifier = Modifier.size(124.dp).clip(CircleShape).background(CGreen.copy(alpha = ringAlpha)))
                        Box(modifier = Modifier.size(116.dp).clip(CircleShape).background(ComicGlassBg))
                        Box(modifier = Modifier.size(108.dp).clip(CircleShape).background(ComicGlassBg), contentAlignment = Alignment.Center) {
                            if (!profilePicUrl.isNullOrBlank()) { AsyncImage(model = profilePicUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(profilePicUrl).fillMaxSize().clip(CircleShape)) }
                            else { Icon(Icons.Filled.Person, contentDescription = null, tint = ScrapbookDark.copy(alpha = 0.4f), modifier = Modifier.size(44.dp)) }
                        }
                    }
                }
                item {
                    Column(modifier = Modifier.fillMaxWidth().offset(y = (-55).dp).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(username.uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 28.sp, letterSpacing = 2.sp, textAlign = TextAlign.Center)
                        if (userHandle.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.92f)).border(1.dp, ScrapbookDark, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 3.dp)) { Text(userHandle, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp) }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ScrapbookStatCard(formatCount(followersCount), "FOLLOWERS", 0.6f) {}
                            ScrapbookStatCard(formatCount(followingCount), "FOLLOWING", 0.6f) {}
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        if (currentUser != null && currentUser?.uid != userId) {
                            var followLoading by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                    .background(if (isFollowing) ScrapbookDark else CGreen)
                                    .border(2.dp, if (isFollowing) CGreen.copy(alpha = 0.5f) else ScrapbookBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (!followLoading) {
                                            followLoading = true
                                            val db = FirebaseFirestore.getInstance()
                                            val myUid = currentUser?.uid ?: return@clickable
                                            if (isFollowing) {
                                                db.collection("users").document(myUid).collection("following").document(userId).delete()
                                                db.collection("users").document(userId).collection("followers").document(myUid).delete()
                                                db.collection("users").document(userId).update("followersCount", com.google.firebase.firestore.FieldValue.increment(-1))
                                                db.collection("users").document(myUid).update("followingCount", com.google.firebase.firestore.FieldValue.increment(-1))
                                                isFollowing = false; followersCount -= 1
                                            } else {
                                                db.collection("users").document(myUid).collection("following").document(userId).set(mapOf("userId" to userId, "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
                                                db.collection("users").document(userId).collection("followers").document(myUid).set(mapOf("userId" to myUid, "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
                                                db.collection("users").document(userId).update("followersCount", com.google.firebase.firestore.FieldValue.increment(1))
                                                db.collection("users").document(myUid).update("followingCount", com.google.firebase.firestore.FieldValue.increment(1))
                                                isFollowing = true; followersCount += 1
                                            }
                                            followLoading = false
                                        }
                                    }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(if (isFollowing) Icons.Filled.PersonRemove else Icons.Filled.PersonAdd, contentDescription = null, tint = if (isFollowing) CGreen else ScrapbookDark, modifier = Modifier.size(18.dp))
                                    Text(if (isFollowing) "FOLLOWING" else "FOLLOW", fontFamily = BangersFontFamily, fontSize = 18.sp, color = if (isFollowing) CGreen else ScrapbookDark, letterSpacing = 1.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        if (bio.isNotBlank()) { BioCard(bioText = bio, habboUsername = habboUsername, habboRegion = habboRegion); Spacer(modifier = Modifier.height(12.dp)) }
                        if (location.isNotBlank() || website.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (location.isNotBlank()) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = ScrapbookTextMuted, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text(location, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
                                if (website.isNotBlank()) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).clickable { val url = if (website.startsWith("http")) website else "https://$website"; try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) { } }) { Icon(Icons.Filled.Language, contentDescription = null, tint = ScrapbookTextMuted, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text(website, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        val platforms = listOf(gamingPlatforms[0] to psnUsername, gamingPlatforms[1] to xboxUsername, gamingPlatforms[2] to steamUsername, gamingPlatforms[3] to nintendoUsername).filter { it.second.isNotBlank() }
                        if (platforms.isNotEmpty()) { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp), modifier = Modifier.fillMaxWidth()) { items(platforms) { (platform, username2) -> PlatformBubble(platform, username2) } }; Spacer(modifier = Modifier.height(8.dp)) }
                        StreamBubbles(twitchUsername, youtubeUsername, context)
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

// ─── Follow List Screen ───────────────────────────────────────────────────────

enum class FollowListType { FOLLOWERS, FOLLOWING }

// ─── Top Games Editor ─────────────────────────────────────────────────────────

@Composable
fun TopGamesEditorContent(currentGames: List<Map<String, Any>>, onGamesChanged: (List<Map<String, Any>>) -> Unit) {
    var editableGames by remember(currentGames) { mutableStateOf(currentGames.toMutableList() as MutableList<Map<String, Any>>) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<IGDBGame>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            delay(600); isSearching = true
            try { searchResults = IGDBRepository.searchGames(searchQuery).take(6) } catch (e: Exception) { searchResults = emptyList() }
            isSearching = false
        } else { searchResults = emptyList() }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${editableGames.size}/7 GAMES", fontFamily = BangersFontFamily, color = CAcPurple, fontSize = 13.sp)
            if (editableGames.size < 7) {
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(CAcPurple.copy(alpha = 0.15f)).border(1.dp, CAcPurple.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).clickable { showSearch = !showSearch }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = CAcPurple, modifier = Modifier.size(14.dp))
                        Text(if (showSearch) "CANCEL" else "ADD GAME", fontFamily = BangersFontFamily, color = CAcPurple, fontSize = 12.sp)
                    }
                }
            }
        }

        AnimatedVisibility(visible = showSearch && editableGames.size < 7, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(ComicGlassBg).border(1.dp, if (searchQuery.isNotBlank()) CAcPurple.copy(alpha = 0.6f) else ScrapbookDark.copy(alpha = 0.3f), RoundedCornerShape(8.dp))) {
                    OutlinedTextField(
                        value = searchQuery, onValueChange = { searchQuery = it },
                        placeholder = { Text("Search for a game...", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.4f), fontSize = 13.sp) },
                        leadingIcon = {
                            if (isSearching) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CAcPurple, strokeWidth = 2.dp)
                            else Icon(Icons.Filled.Search, contentDescription = null, tint = CAcPurple, modifier = Modifier.size(16.dp))
                        },
                        trailingIcon = { if (searchQuery.isNotBlank()) IconButton(onClick = { searchQuery = ""; searchResults = emptyList() }) { Icon(Icons.Filled.Close, contentDescription = null, tint = ScrapbookDark.copy(alpha = 0.5f), modifier = Modifier.size(14.dp)) } },
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookDark),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, cursorColor = CAcPurple),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchResults.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ComicGlassBg).border(1.dp, CAcPurple.copy(alpha = 0.3f), RoundedCornerShape(10.dp))) {
                        searchResults.forEachIndexed { i, game ->
                            val alreadyAdded = editableGames.any { (it["name"] as? String)?.equals(game.name, ignoreCase = true) == true }
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .then(if (!alreadyAdded) Modifier.clickable {
                                        val newGame = mapOf("name" to game.name, "coverUrl" to (game.coverUrl ?: ""))
                                        editableGames = (editableGames + newGame).toMutableList()
                                        onGamesChanged(editableGames)
                                        if (editableGames.size >= 7) showSearch = false
                                        searchQuery = ""; searchResults = emptyList()
                                    } else Modifier)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(ComicGlassBg)) {
                                    if (!game.coverUrl.isNullOrBlank()) AsyncImage(model = game.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(game.coverUrl).fillMaxSize())
                                    else Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(game.name.take(2).uppercase(), fontFamily = BangersFontFamily, color = CAcPurple, fontSize = 12.sp) }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(game.name, fontFamily = BangersFontFamily, color = if (alreadyAdded) ScrapbookDark.copy(alpha = 0.35f) else ScrapbookDark, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    game.releaseYear?.let { Text("$it", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp) }
                                }
                                if (alreadyAdded) Text("ADDED", fontFamily = BangersFontFamily, color = CAcPurple.copy(alpha = 0.5f), fontSize = 10.sp)
                                else Icon(Icons.Filled.Add, contentDescription = null, tint = CAcPurple, modifier = Modifier.size(18.dp))
                            }
                            if (i < searchResults.size - 1) HorizontalDivider(color = ScrapbookDark.copy(alpha = 0.12f))
                        }
                    }
                }
            }
        }

        if (editableGames.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.92f)).border(1.dp, ScrapbookDark.copy(alpha = 0.3f), RoundedCornerShape(10.dp)).padding(20.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🎮", fontSize = 28.sp)
                    Text("No games added yet", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 13.sp)
                    Text("Tap ADD GAME to search", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.45f), fontSize = 11.sp)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                editableGames.forEachIndexed { index, game ->
                    val gameName = game["name"] as? String ?: ""
                    val coverUrl = game["coverUrl"] as? String
                    val accentColor = gameAccentColor(gameName)
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ComicGlassBg).border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)).padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.2f)).border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
                                Text(when (index + 1) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "${index + 1}" }, fontSize = if (index < 3) 12.sp else 10.sp, fontFamily = BangersFontFamily, color = accentColor)
                            }
                            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)).background(accentColor.copy(alpha = 0.15f))) {
                                if (!coverUrl.isNullOrBlank()) AsyncImage(model = coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(coverUrl).fillMaxSize())
                                else Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(gameName.take(2).uppercase(), fontFamily = BangersFontFamily, color = accentColor, fontSize = 14.sp) }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(gameName, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(detectGenreForGame(gameName), fontFamily = NunitoFontFamily, color = accentColor.copy(alpha = 0.7f), fontSize = 11.sp)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                if (index > 0) Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(ScrapbookDark.copy(alpha = 0.08f)).clickable { val newList = editableGames.toMutableList(); val temp = newList[index]; newList[index] = newList[index - 1]; newList[index - 1] = temp; editableGames = newList; onGamesChanged(editableGames) }, contentAlignment = Alignment.Center) { Text("↑", color = ScrapbookDark.copy(alpha = 0.7f), fontSize = 12.sp) }
                                if (index < editableGames.size - 1) Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(ScrapbookDark.copy(alpha = 0.08f)).clickable { val newList = editableGames.toMutableList(); val temp = newList[index]; newList[index] = newList[index + 1]; newList[index + 1] = temp; editableGames = newList; onGamesChanged(editableGames) }, contentAlignment = Alignment.Center) { Text("↓", color = ScrapbookDark.copy(alpha = 0.7f), fontSize = 12.sp) }
                            }
                            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(CAcRed.copy(alpha = 0.15f)).border(1.dp, CAcRed.copy(alpha = 0.4f), CircleShape).clickable { editableGames = editableGames.toMutableList().also { it.removeAt(index) }; onGamesChanged(editableGames) }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove", tint = CAcRed, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
        Text("Changes save when you tap SAVE PROFILE", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.45f), fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

// ─── Top Soundtracks Editor ───────────────────────────────────────────────────

@Composable
fun TopSoundtracksEditorContent(currentSoundtracks: List<Map<String, Any>>, onSoundtracksChanged: (List<Map<String, Any>>) -> Unit) {
    var editableTracks by remember(currentSoundtracks) { mutableStateOf(currentSoundtracks.toMutableList() as MutableList<Map<String, Any>>) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<IGDBGame>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            delay(600); isSearching = true
            try { searchResults = IGDBRepository.searchGames(searchQuery).take(6) } catch (e: Exception) { searchResults = emptyList() }
            isSearching = false
        } else { searchResults = emptyList() }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${editableTracks.size}/3 SOUNDTRACKS", fontFamily = BangersFontFamily, color = CAcRed, fontSize = 13.sp)
            if (editableTracks.size < 3) {
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(CAcRed.copy(alpha = 0.15f)).border(1.dp, CAcRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).clickable { showSearch = !showSearch }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = CAcRed, modifier = Modifier.size(14.dp))
                        Text(if (showSearch) "CANCEL" else "ADD SOUNDTRACK", fontFamily = BangersFontFamily, color = CAcRed, fontSize = 12.sp)
                    }
                }
            }
        }

        AnimatedVisibility(visible = showSearch && editableTracks.size < 3, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(ComicGlassBg).border(1.dp, if (searchQuery.isNotBlank()) CAcRed.copy(alpha = 0.6f) else ScrapbookDark.copy(alpha = 0.3f), RoundedCornerShape(8.dp))) {
                    OutlinedTextField(
                        value = searchQuery, onValueChange = { searchQuery = it },
                        placeholder = { Text("Search game soundtrack...", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.4f), fontSize = 13.sp) },
                        leadingIcon = {
                            if (isSearching) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CAcRed, strokeWidth = 2.dp)
                            else Icon(Icons.Filled.Search, contentDescription = null, tint = CAcRed, modifier = Modifier.size(16.dp))
                        },
                        trailingIcon = { if (searchQuery.isNotBlank()) IconButton(onClick = { searchQuery = ""; searchResults = emptyList() }) { Icon(Icons.Filled.Close, contentDescription = null, tint = ScrapbookDark.copy(alpha = 0.5f), modifier = Modifier.size(14.dp)) } },
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookDark),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, cursorColor = CAcRed),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchResults.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ComicGlassBg).border(1.dp, CAcRed.copy(alpha = 0.3f), RoundedCornerShape(10.dp))) {
                        searchResults.forEachIndexed { i, game ->
                            val alreadyAdded = editableTracks.any { (it["name"] as? String)?.equals("${game.name} OST", ignoreCase = true) == true }
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .then(if (!alreadyAdded) Modifier.clickable {
                                        val newTrack = mapOf("name" to "${game.name} OST", "gameName" to game.name, "coverUrl" to (game.coverUrl ?: ""))
                                        editableTracks = (editableTracks + newTrack).toMutableList()
                                        onSoundtracksChanged(editableTracks)
                                        if (editableTracks.size >= 3) showSearch = false
                                        searchQuery = ""; searchResults = emptyList()
                                    } else Modifier)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(ComicGlassBg)) {
                                    if (!game.coverUrl.isNullOrBlank()) AsyncImage(model = game.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(game.coverUrl).fillMaxSize().clip(CircleShape))
                                    else Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("🎵", fontSize = 16.sp) }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${game.name} OST", fontFamily = BangersFontFamily, color = if (alreadyAdded) ScrapbookDark.copy(alpha = 0.35f) else ScrapbookDark, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Soundtrack", fontFamily = NunitoFontFamily, color = CAcRed.copy(alpha = 0.6f), fontSize = 11.sp)
                                }
                                if (alreadyAdded) Text("ADDED", fontFamily = BangersFontFamily, color = CAcRed.copy(alpha = 0.5f), fontSize = 10.sp)
                                else Icon(Icons.Filled.Add, contentDescription = null, tint = CAcRed, modifier = Modifier.size(18.dp))
                            }
                            if (i < searchResults.size - 1) HorizontalDivider(color = ScrapbookDark.copy(alpha = 0.12f))
                        }
                    }
                }
            }
        }

        if (editableTracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.92f)).border(1.dp, ScrapbookDark.copy(alpha = 0.3f), RoundedCornerShape(10.dp)).padding(20.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🎵", fontSize = 28.sp)
                    Text("No soundtracks added yet", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 13.sp)
                    Text("Tap ADD SOUNDTRACK to search", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.45f), fontSize = 11.sp)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                editableTracks.forEachIndexed { index, track ->
                    val trackName = track["name"] as? String ?: ""
                    val gameName = track["gameName"] as? String ?: ""
                    val coverUrl = track["coverUrl"] as? String
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ComicGlassBg).border(1.dp, CAcRed.copy(alpha = 0.35f), RoundedCornerShape(10.dp)).padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(ScrapbookDark).border(1.dp, CAcRed.copy(alpha = 0.4f), CircleShape)) {
                                if (!coverUrl.isNullOrBlank()) AsyncImage(model = coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(coverUrl).fillMaxSize().clip(CircleShape))
                                else Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("🎵", fontSize = 18.sp) }
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(ScrapbookDark).align(Alignment.Center))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(trackName, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (gameName.isNotBlank()) Text(gameName, fontFamily = NunitoFontFamily, color = CAcRed.copy(alpha = 0.6f), fontSize = 11.sp)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                if (index > 0) Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(ScrapbookDark.copy(alpha = 0.08f)).clickable { val newList = editableTracks.toMutableList(); val temp = newList[index]; newList[index] = newList[index - 1]; newList[index - 1] = temp; editableTracks = newList; onSoundtracksChanged(editableTracks) }, contentAlignment = Alignment.Center) { Text("↑", color = ScrapbookDark.copy(alpha = 0.7f), fontSize = 12.sp) }
                                if (index < editableTracks.size - 1) Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(ScrapbookDark.copy(alpha = 0.08f)).clickable { val newList = editableTracks.toMutableList(); val temp = newList[index]; newList[index] = newList[index + 1]; newList[index + 1] = temp; editableTracks = newList; onSoundtracksChanged(editableTracks) }, contentAlignment = Alignment.Center) { Text("↓", color = ScrapbookDark.copy(alpha = 0.7f), fontSize = 12.sp) }
                            }
                            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(CAcRed.copy(alpha = 0.15f)).border(1.dp, CAcRed.copy(alpha = 0.4f), CircleShape).clickable { editableTracks = editableTracks.toMutableList().also { it.removeAt(index) }; onSoundtracksChanged(editableTracks) }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove", tint = CAcRed, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
        Text("Changes save when you tap SAVE PROFILE", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.45f), fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
@Composable
fun GameDetailBottomSheet(game: Game, index: Int, topGenre: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val accentColor = gameAccentColor(game.name)
    val genre = detectGenreForGame(game.name)
    val isTopGenreMatch = genre.equals(topGenre, ignoreCase = true)
    var igdbData by remember { mutableStateOf<IGDBGame?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(game.name) { try { val results = IGDBRepository.searchGames(game.name); igdbData = results.firstOrNull() } catch (e: Exception) { }; isLoading = false }
    val neonAlpha by rememberGlowRange(0.4f, 1f)
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable { onDismiss() }, contentAlignment = Alignment.BottomCenter) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.75f).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(ComicGlassBg).border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).clickable { }) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                    val coverUrl = igdbData?.coverUrl ?: game.coverUrl
                    if (!coverUrl.isNullOrBlank()) { AsyncImage(model = coverUrl, contentDescription = game.name, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(coverUrl).fillMaxSize()) }
                    else if (game.imageResId != null) { Image(painterResource(game.imageResId), game.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                    else { Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(accentColor.copy(alpha = 0.4f), ComicGlassBg))), contentAlignment = Alignment.Center) { Text(game.name.take(2).uppercase(), fontFamily = BangersFontFamily, color = accentColor, fontSize = 56.sp) } }
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.95f)))))
                    Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp).width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookDark.copy(alpha = 0.3f)))
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.8f)).clickable { onDismiss() }, contentAlignment = Alignment.Center) { Text("✕", color = ScrapbookDark, fontSize = 14.sp) }
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            val medalText = when (index) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "#${index + 1}" }
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(accentColor.copy(alpha = 0.2f)).border(1.dp, accentColor, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) { Text(medalText, fontFamily = BangersFontFamily, color = accentColor, fontSize = 14.sp) }
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(accentColor.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp)) { Text(genre, fontFamily = BangersFontFamily, color = accentColor, fontSize = 12.sp) }
                            if (isTopGenreMatch) { Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(CGreen.copy(alpha = 0.2f)).border(1.dp, CGreen, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) { Text("⭐ DEFINES YOU", fontFamily = BangersFontFamily, color = CGreen, fontSize = 11.sp) } }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(game.name, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 26.sp, lineHeight = 30.sp)
                    }
                }
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isLoading) { Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { ThreeDotsAnimation() } }
                    else {
                        igdbData?.let { data ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                data.rating?.let { rating -> Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(accentColor.copy(alpha = 0.15f)).border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${rating.toInt()}", fontFamily = BangersFontFamily, color = accentColor, fontSize = 28.sp); Text("RATING", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.55f), fontSize = 10.sp, letterSpacing = 1.sp) } } }
                                data.releaseYear?.let { year -> Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.92f)).border(1.dp, ScrapbookDark.copy(alpha = 0.3f), RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$year", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 28.sp); Text("RELEASED", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.55f), fontSize = 10.sp, letterSpacing = 1.sp) } } }
                            }
                            data.summary?.let { summary ->
                                Spacer(modifier = Modifier.height(14.dp))
                                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.92f)).border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp)).padding(14.dp)) {
                                    Column { Text("ABOUT", fontFamily = BangersFontFamily, color = accentColor, fontSize = 13.sp, letterSpacing = 1.sp); Spacer(modifier = Modifier.height(6.dp)); Text(summary, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.8f), fontSize = 14.sp, lineHeight = 21.sp, maxLines = 5, overflow = TextOverflow.Ellipsis) }
                                }
                            }
                        } ?: Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.92f)).padding(16.dp), contentAlignment = Alignment.Center) { Text("No database info found", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.55f), fontSize = 13.sp) }
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

@Composable
fun HeroGameCard(game: Game, topGenre: String, onTap: () -> Unit) {
    val accentColor = gameAccentColor(game.name)
    val genre = detectGenreForGame(game.name)
    val neonAlpha by rememberGlowRange(0.4f, 1f)
    val kbT = rememberInfiniteTransition(label = "heroKB")
    val kbScale by kbT.animateFloat(initialValue = 1.0f, targetValue = 1.08f, animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse), label = "heroKBScale")
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "heroPressScale")
    Box(modifier = Modifier.fillMaxWidth().scale(pressScale).clip(RoundedCornerShape(16.dp)).border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(16.dp)).clickable { pressed = true; onTap() }) {
        Box(modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp))) {
            when {
                game.coverUrl != null -> AsyncImage(model = game.coverUrl, contentDescription = game.name, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(game.coverUrl).fillMaxSize().scale(kbScale))
                game.imageResId != null -> Image(painterResource(game.imageResId), game.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().scale(kbScale))
                else -> Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(accentColor.copy(alpha = 0.5f), ScrapbookDark))), contentAlignment = Alignment.Center) { Text(game.name.take(2).uppercase(), fontFamily = BangersFontFamily, color = accentColor, fontSize = 52.sp) }
            }
            Column(modifier = Modifier.fillMaxSize()) { repeat(35) { Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.Black.copy(alpha = 0.04f))); Spacer(modifier = Modifier.height(2.dp)) } }
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
            Box(modifier = Modifier.align(Alignment.TopStart).padding(10.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFFFD700).copy(alpha = 0.9f)).border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Text("👑", fontSize = 12.sp); Text("#1 FAVORITE", fontFamily = BangersFontFamily, color = Color.White, fontSize = 12.sp) } }
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).clip(RoundedCornerShape(8.dp)).background(accentColor.copy(alpha = 0.85f)).padding(horizontal = 8.dp, vertical = 4.dp)) { Text(genre, fontFamily = BangersFontFamily, color = Color.White, fontSize = 11.sp) }
            if (genre.equals(topGenre, ignoreCase = true)) { Box(modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).clip(RoundedCornerShape(8.dp)).background(CGreen.copy(alpha = 0.9f)).padding(horizontal = 8.dp, vertical = 4.dp)) { Text("⭐ DEFINES YOU", fontFamily = BangersFontFamily, color = Color.White, fontSize = 10.sp) } }
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Text(game.name, fontFamily = BangersFontFamily, color = Color.White, fontSize = 24.sp, letterSpacing = 0.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("TAP FOR DETAILS →", fontFamily = BangersFontFamily, color = accentColor.copy(alpha = 0.8f), fontSize = 11.sp, letterSpacing = 1.sp)
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(180); pressed = false } }
}

@Composable
fun GameItem(game: Game, index: Int = 0, topGenre: String = "", onTap: () -> Unit = {}, modifier: Modifier = Modifier) {
    val accentColor = gameAccentColor(game.name)
    val genre = detectGenreForGame(game.name)
    val isPersonalityMatch = genre.equals(topGenre, ignoreCase = true)
    val neonAlpha by rememberGlowRange(0.3f, 0.9f)
    val kbT = rememberInfiniteTransition(label = "gameKB_$index")
    val kbScale by kbT.animateFloat(initialValue = 1.05f, targetValue = 1.14f, animationSpec = infiniteRepeatable(keyframes { durationMillis = 12000 + index * 1500; 1.05f at 0; 1.14f at (6000 + index * 750); 1.05f at (12000 + index * 1500) }, RepeatMode.Restart), label = "gameKBScale_$index")
    var visible by remember { mutableStateOf(false) }
    val enterOffset by animateFloatAsState(targetValue = if (visible) 0f else 50f, animationSpec = tween(500, delayMillis = index * 80, easing = LinearOutSlowInEasing), label = "gameEnterOffset_$index")
    val enterAlpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(500, delayMillis = index * 80, easing = LinearOutSlowInEasing), label = "gameEnterAlpha_$index")
    LaunchedEffect(Unit) { visible = true }
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(targetValue = if (pressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "gamePress_$index")
    Box(modifier = modifier.offset(y = enterOffset.dp).graphicsLayer { alpha = enterAlpha }.scale(pressScale).clip(RoundedCornerShape(12.dp)).border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(12.dp)).clickable { pressed = true; onTap() }) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ComicGlassBg, cornerRadius = 12.dp, shadowOffset = 4.dp) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(GAME_IMAGE_HEIGHT).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))) {
                    when {
                        game.coverUrl != null -> AsyncImage(model = game.coverUrl, contentDescription = game.name, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(game.coverUrl).fillMaxSize().scale(kbScale))
                        game.imageResId != null -> Image(painterResource(game.imageResId), game.name, modifier = Modifier.fillMaxSize().scale(kbScale), contentScale = ContentScale.Crop)
                        else -> Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(accentColor.copy(alpha = 0.4f), ScrapbookDark))), contentAlignment = Alignment.Center) { Text(game.name.take(2).uppercase(), fontFamily = BangersFontFamily, color = accentColor, fontSize = 32.sp) }
                    }
                    Column(modifier = Modifier.fillMaxSize()) { repeat(20) { Box(modifier = Modifier.fillMaxWidth().height(5.dp).background(Color.Black.copy(alpha = 0.04f))); Spacer(modifier = Modifier.height(3.dp)) } }
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)))))
                    val medalText = when (index + 1) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "${index + 1}" }
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.85f)).border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 3.dp)) { Text(medalText, fontFamily = BangersFontFamily, color = accentColor, fontSize = 12.sp) }
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(6.dp)).background(accentColor.copy(alpha = 0.85f)).padding(horizontal = 6.dp, vertical = 3.dp)) { Text(genre, fontFamily = BangersFontFamily, color = Color.White, fontSize = 8.sp) }
                    if (isPersonalityMatch) { Box(modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).clip(RoundedCornerShape(6.dp)).background(CGreen.copy(alpha = 0.9f)).padding(horizontal = 5.dp, vertical = 2.dp)) { Text("⭐", fontSize = 10.sp) } }
                }
                Box(modifier = Modifier.fillMaxWidth().height(GAME_LABEL_HEIGHT).background(Brush.horizontalGradient(colors = listOf(accentColor.copy(alpha = 0.15f), Color.White.copy(alpha = 0.6f), accentColor.copy(alpha = 0.15f)))).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                    Text(game.name, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(180); pressed = false } }
}

// ── Console branding for physical game boxes ──────────────────────────────────

data class ConsoleBranding(
    val topBarColors: List<Color>,
    val spineColors: List<Color>,
    val topLabel: String,
    val spineLabel: String,
    val subLabel: String,
    val subLabel2: String = ""
)

fun inferPlatformFromName(gameName: String): String? {
    val lower = gameName.lowercase()
    return when {
        "wii u"         in lower -> "Wii U"
        "wii"           in lower -> "Wii"
        "nintendo switch" in lower -> "Nintendo Switch"
        "3ds"           in lower -> "Nintendo 3DS"
        "nintendo ds"   in lower -> "Nintendo DS"
        "game boy advance" in lower || " gba " in lower -> "Game Boy Advance"
        "gamecube"      in lower || "game cube" in lower -> "Nintendo GameCube"
        "n64"           in lower || "nintendo 64" in lower -> "Nintendo 64"
        "snes"          in lower || "super nintendo" in lower -> "Super Nintendo"
        "ps5"           in lower -> "PlayStation 5"
        "ps4"           in lower -> "PlayStation 4"
        "ps3"           in lower -> "PlayStation 3"
        "ps2"           in lower -> "PlayStation 2"
        "ps1"           in lower || "psx"  in lower -> "PlayStation"
        "psp"           in lower -> "PSP"
        "vita"          in lower -> "PS Vita"
        "xbox series"   in lower -> "Xbox Series X"
        "xbox one"      in lower -> "Xbox One"
        "xbox 360"      in lower -> "Xbox 360"
        "xbox"          in lower -> "Xbox"
        "dreamcast"     in lower -> "Dreamcast"
        "saturn"        in lower -> "Sega Saturn"
        "genesis"       in lower || "mega drive" in lower -> "Sega Genesis"
        else -> null
    }
}

fun getPlatformBranding(platform: String?): ConsoleBranding {
    val p = (platform ?: "").lowercase()
    return when {
        "playstation 5" in p || "ps5" in p -> ConsoleBranding(
            listOf(CAcBlue, CAcBlue, CAcBlue),
            listOf(CAcBlueD, CAcBlue, CAcBlueD),
            "PS5", "PLAYSTATION 5", "PlayStation®5", "Ultra HD Blu-ray™")
        "playstation 4" in p || "ps4" in p -> ConsoleBranding(
            listOf(CAcBlue, CAcBlue, CAcBlue),
            listOf(Color(0xFF0A2D6E), CAcBlue, Color(0xFF0A2D6E)),
            "PS4", "PLAYSTATION 4", "PlayStation®4")
        "playstation 3" in p || "ps3" in p -> ConsoleBranding(
            listOf(CAcBlue, CAcBlue, CAcBlue),
            listOf(Color(0xFF0A2D6E), CAcBlue, Color(0xFF0A2D6E)),
            "PS3", "PLAYSTATION 3", "PlayStation®3", "Blu-ray Disc™")
        "playstation 2" in p || "ps2" in p -> ConsoleBranding(
            listOf(CAcBlue, Color(0xFF0050A0), CAcBlue),
            listOf(Color(0xFF000050), Color(0xFF000078), Color(0xFF000050)),
            "PS2", "PLAYSTATION 2", "PlayStation®2", "DVD ROM")
        "psp" in p -> ConsoleBranding(
            listOf(CAcBlue, CAcBlue, CAcBlue),
            listOf(Color(0xFF0A2D6E), CAcBlue, Color(0xFF0A2D6E)),
            "PSP", "PSP™", "PSP™", "UMD™")
        "playstation" in p || "ps1" in p || "psx" in p -> ConsoleBranding(
            listOf(Color(0xFF808080), Color(0xFFA0A0A0), Color(0xFF808080)),
            listOf(Color(0xFF505050), Color(0xFF707070), Color(0xFF505050)),
            "PS1", "PLAYSTATION", "PlayStation®")
        "xbox series" in p -> ConsoleBranding(
            listOf(Color(0xFF107C10), Color(0xFF1DB954), Color(0xFF107C10)),
            listOf(Color(0xFF0A5A0A), Color(0xFF107C10), Color(0xFF0A5A0A)),
            "XSX", "XBOX SERIES X", "Xbox Series X")
        "xbox one" in p -> ConsoleBranding(
            listOf(Color(0xFF107C10), Color(0xFF1DB954), Color(0xFF107C10)),
            listOf(Color(0xFF0A5A0A), Color(0xFF107C10), Color(0xFF0A5A0A)),
            "ONE", "XBOX ONE", "Xbox One")
        "xbox 360" in p -> ConsoleBranding(
            listOf(Color(0xFF107C10), Color(0xFF52B043), Color(0xFF107C10)),
            listOf(Color(0xFF0A5A0A), Color(0xFF107C10), Color(0xFF0A5A0A)),
            "X360", "XBOX 360", "Xbox 360", "HD DVD")
        "xbox" in p -> ConsoleBranding(
            listOf(Color(0xFF107C10), Color(0xFF52B043), Color(0xFF107C10)),
            listOf(Color(0xFF0A5A0A), Color(0xFF107C10), Color(0xFF0A5A0A)),
            "XBOX", "MICROSOFT XBOX", "Microsoft Xbox", "DVD-ROM")
        "wii u" in p -> ConsoleBranding(
            listOf(Color(0xFF009AC7), Color(0xFF00BFEF), Color(0xFF009AC7)),
            listOf(Color(0xFF006A8A), Color(0xFF009AC7), Color(0xFF006A8A)),
            "WiiU", "NINTENDO WII U", "Nintendo Wii U™")
        "wii" in p -> ConsoleBranding(
            listOf(Color(0xFFE4000F), Color(0xFFFF3333), Color(0xFFE4000F)),
            listOf(CAcRedD, CAcRed, CAcRedD),
            "Wii", "NINTENDO Wii™", "Nintendo Wii™")
        "switch" in p -> ConsoleBranding(
            listOf(Color(0xFFE4000F), Color(0xFFFF3333), Color(0xFFE4000F)),
            listOf(CAcRedD, CAcRed, CAcRedD),
            "NSW", "NINTENDO SWITCH", "Nintendo Switch™")
        "nintendo 64" in p || "n64" in p -> ConsoleBranding(
            listOf(Color(0xFF1E1E96), Color(0xFF3232C8), Color(0xFF1E1E96)),
            listOf(Color(0xFF000064), Color(0xFF1E1E96), Color(0xFF000064)),
            "N64", "NINTENDO 64", "Nintendo 64™")
        "gamecube" in p || "game cube" in p -> ConsoleBranding(
            listOf(Color(0xFF4B0082), Color(0xFF7B2FBE), Color(0xFF4B0082)),
            listOf(Color(0xFF3B0072), Color(0xFF5B0092), Color(0xFF3B0072)),
            "GCN", "NINTENDO GAMECUBE", "Nintendo GameCube™")
        "game boy advance" in p || "gba" in p -> ConsoleBranding(
            listOf(Color(0xFF9B0AC8), Color(0xFFBF32E8), Color(0xFF9B0AC8)),
            listOf(Color(0xFF7B00A8), Color(0xFF9B0AC8), Color(0xFF7B00A8)),
            "GBA", "GAME BOY ADVANCE", "Game Boy Advance™")
        "dreamcast" in p -> ConsoleBranding(
            listOf(Color(0xFF1846A0), Color(0xFF2858C0), Color(0xFF1846A0)),
            listOf(Color(0xFF0A3080), Color(0xFF1846A0), Color(0xFF0A3080)),
            "DC", "SEGA DREAMCAST", "Sega Dreamcast™", "GD-ROM")
        "saturn" in p -> ConsoleBranding(
            listOf(Color(0xFF1A1A6E), Color(0xFF2A2A8E), Color(0xFF1A1A6E)),
            listOf(Color(0xFF0A0A5E), Color(0xFF1A1A6E), Color(0xFF0A0A5E)),
            "SAT", "SEGA SATURN", "Sega Saturn™", "CD-ROM")
        "genesis" in p || "mega drive" in p -> ConsoleBranding(
            listOf(ScrapbookDark, ScrapbookDark, ScrapbookDark),
            listOf(ScrapbookDark, Color.White.copy(alpha = 0.44f), ScrapbookDark),
            "GEN", "SEGA GENESIS", "Sega Genesis™")
        "pc" in p || "windows" in p || "steam" in p -> ConsoleBranding(
            listOf(CGreenDeep, Color(0xFF2A475E), CGreenDeep),
            listOf(Color(0xFF0E1A24), CGreenDeep, Color(0xFF0E1A24)),
            "PC", "PC / STEAM", "PC / Steam™")
        else -> ConsoleBranding(  // default fallback — PS3
            listOf(CAcBlue, CAcBlue, CAcBlue),
            listOf(Color(0xFF0A2D6E), CAcBlue, Color(0xFF0A2D6E)),
            "PS3", "PLAYSTATION 3", "PlayStation®3", "Blu-ray Disc™")
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PhysicalGameBoxCard(game: Game, index: Int, topGenre: String, modifier: Modifier = Modifier, onTap: () -> Unit) {
    val accentColor = gameAccentColor(game.name)
    val medalText = when (index + 1) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> null }
    val effectivePlatform = game.platform ?: inferPlatformFromName(game.name)
    val branding = getPlatformBranding(effectivePlatform)
    // Each slot gets a small fixed tilt — scattered physical feel
    val tiltZ = listOf(-3.5f, 2.8f, -1.5f, 4f, -2.8f, 1.8f, -3f).getOrElse(index % 7) { 0f }
    // Alternating vertical stagger so boxes look like they're at different heights
    val staggerY = if (index % 2 == 0) 10.dp else (-10).dp

    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "ps3Press_$index")
    val tiltY by animateFloatAsState(targetValue = if (pressed) -15f else 0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium), label = "ps3TiltY_$index")

    Box(modifier = modifier
        .offset(y = staggerY)
        .graphicsLayer {
            rotationZ = tiltZ
            rotationY = tiltY
            scaleX = pressScale; scaleY = pressScale
            cameraDistance = 10f * density
        }
        .width(118.dp)
        .aspectRatio(0.72f)
        .clickable { pressed = true; onTap() }
    ) {
        // Multi-layer 3D drop shadow
        Box(modifier = Modifier.matchParentSize().offset(x = 7.dp, y = 9.dp).clip(RoundedCornerShape(4.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
        Box(modifier = Modifier.matchParentSize().offset(x = 3.dp, y = 4.dp).clip(RoundedCornerShape(4.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
        // PHYSICAL CASE SHELL — plastic silver border
        Box(modifier = Modifier.fillMaxSize()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFA0AAB4))
            .border(1.5.dp, Color(0xFF707880), RoundedCornerShape(4.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(2.dp).clip(RoundedCornerShape(2.dp))) {

                // CONSOLE-BRANDED TOP BAR
                Box(modifier = Modifier.fillMaxWidth().height(22.dp)
                    .background(Brush.horizontalGradient(branding.topBarColors))
                ) {
                    Text(branding.topLabel, fontFamily = BangersFontFamily, color = Color.White, fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 7.dp))
                    Column(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 5.dp),
                        horizontalAlignment = Alignment.End) {
                        Text(branding.subLabel, fontFamily = NunitoFontFamily, color = Color.White.copy(0.85f), fontSize = 5.5.sp)
                        if (branding.subLabel2.isNotEmpty())
                            Text(branding.subLabel2, fontFamily = NunitoFontFamily, color = Color.White.copy(0.4f), fontSize = 4.sp)
                    }
                    if (medalText != null) Text(medalText, fontSize = 10.sp, modifier = Modifier.align(Alignment.Center))
                }

                // SPINE + COVER ART
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    // LEFT SPINE — console-branded color with rotated console name
                    Box(modifier = Modifier.width(15.dp).fillMaxHeight()
                        .background(Brush.verticalGradient(branding.spineColors))
                    ) {
                        Text(branding.spineLabel, fontFamily = BangersFontFamily, color = Color.White.copy(0.85f),
                            fontSize = 5.5.sp, letterSpacing = 0.8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.Center).graphicsLayer { rotationZ = -90f })
                    }
                    // GAME COVER — fills to the right of the spine
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth().padding(start = 15.dp)) {
                        when {
                            game.coverUrl != null -> AsyncImage(model = game.coverUrl, contentDescription = game.name,
                                contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(game.coverUrl).fillMaxSize())
                            game.imageResId != null -> Image(painterResource(game.imageResId), game.name,
                                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            else -> Box(modifier = Modifier.fillMaxSize()
                                .background(Brush.verticalGradient(listOf(accentColor.copy(0.6f), Color(0xFF080814)))),
                                contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(game.name.take(2).uppercase(), fontFamily = BangersFontFamily, color = accentColor, fontSize = 28.sp)
                                    Text(game.name, fontFamily = BangersFontFamily, color = Color.White.copy(0.7f), fontSize = 7.sp,
                                        textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 4.dp), maxLines = 2)
                                }
                            }
                        }
                        // ESRB rating badge bottom-left
                        Box(modifier = Modifier.align(Alignment.BottomStart).padding(3.dp).size(13.dp)
                            .background(Color.White).border(0.5.dp, Color.Black, RoundedCornerShape(1.dp)),
                            contentAlignment = Alignment.Center) {
                            Text("T", fontFamily = BangersFontFamily, color = Color.Black, fontSize = 8.sp)
                        }
                        // Plastic wrap gloss shine
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(brush = Brush.linearGradient(
                                colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.38f)
                            ))
                        }
                    }
                }

                // BOTTOM BLACK BAR
                Box(modifier = Modifier.fillMaxWidth().height(15.dp).background(Color(0xFF060606))) {
                    Text(game.name.take(24).uppercase(), fontFamily = BangersFontFamily, color = Color.White.copy(0.5f),
                        fontSize = 5.sp, letterSpacing = 0.4.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 17.dp, end = 4.dp))
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(180); pressed = false } }
}

@Composable
fun TopGamesSection(games: List<Game>, topGenre: String = "") {
    if (games.isEmpty()) return
    var selectedGameIndex by remember { mutableStateOf<Int?>(null) }
    val baseList = games.take(7)
    // Triple the list for seamless infinite loop
    val loopList = remember(baseList) { baseList + baseList + baseList }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = baseList.size)

    // Auto-scroll to the left, continuously
    LaunchedEffect(listState) {
        while (true) {
            delay(16L)
            listState.scrollBy(1.4f)
            // When we reach the end of the second copy, jump back to the same spot in the first copy — seamless
            if (listState.firstVisibleItemIndex >= baseList.size * 2) {
                listState.scrollToItem(
                    index = listState.firstVisibleItemIndex - baseList.size,
                    scrollOffset = listState.firstVisibleItemScrollOffset
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)) {
            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                .background(CGreen.copy(alpha = 0.15f))
                .border(1.dp, CGreen.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text("PHYSICAL COLLECTION", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 10.sp, letterSpacing = 1.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("tap a box to open", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
        }
        // Marquee — extra vertical padding so rotated/staggered boxes don't clip
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            userScrollEnabled = true
        ) {
            itemsIndexed(loopList) { loopIndex, game ->
                val gameIndex = loopIndex % baseList.size
                PhysicalGameBoxCard(
                    game = game,
                    index = gameIndex,
                    topGenre = topGenre,
                    modifier = Modifier
                ) { selectedGameIndex = gameIndex }
            }
        }
    }
    selectedGameIndex?.let { idx ->
        val game = games.getOrNull(idx)
        if (game != null) {
            Dialog(
                onDismissRequest = { selectedGameIndex = null },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
            ) {
                Box(modifier = Modifier.comicPop()) {
                    GameDetailBottomSheet(
                        game = game, index = idx, topGenre = topGenre,
                        onDismiss = { selectedGameIndex = null }
                    )
                }
            }
        }
    }
}

@Composable
fun SoundtrackReactionBar(soundtrackTitle: String, currentUserId: String) {
    val initialReactions: Map<String, Int> = mapOf("🔥" to 0, "❤️" to 0, "🎵" to 0)
    var reactions by remember { mutableStateOf(initialReactions) }
    var userReacted by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(soundtrackTitle) {
        try {
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("soundtrackReactions").document(soundtrackTitle.hashCode().toString()).get().await()
            if (doc.exists()) {
                val data = doc.data ?: emptyMap<String, Any>()
                reactions = mapOf("🔥" to ((data["fire"] as? Long)?.toInt() ?: 0), "❤️" to ((data["heart"] as? Long)?.toInt() ?: 0), "🎵" to ((data["music"] as? Long)?.toInt() ?: 0))
                userReacted = data["user_$currentUserId"] as? String
            }
        } catch (e: Exception) { }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("🔥", "❤️", "🎵").forEach { emoji ->
            val count = reactions[emoji] ?: 0; val isReacted = userReacted == emoji
            var popped by remember { mutableStateOf(false) }
            val popScale by animateFloatAsState(targetValue = if (popped) 1.4f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh), label = "soundtrackPop_$emoji")
            Box(modifier = Modifier.scale(popScale).clip(RoundedCornerShape(20.dp)).background(if (isReacted) CGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.46f)).border(1.5.dp, if (isReacted) CGreen.copy(alpha = 0.8f) else ScrapbookDark.copy(alpha = 0.3f), RoundedCornerShape(20.dp)).clickable {
                popped = true
                val newReacted = if (userReacted == emoji) null else emoji; val prev = userReacted; userReacted = newReacted
                reactions = reactions.toMutableMap().apply { prev?.let { this[it] = (this[it] ?: 1) - 1 }; newReacted?.let { this[it] = (this[it] ?: 0) + 1 } }
                try {
                    val db = FirebaseFirestore.getInstance(); val docRef = db.collection("soundtrackReactions").document(soundtrackTitle.hashCode().toString())
                    val updates = mutableMapOf<String, Any>("fire" to (reactions["🔥"] ?: 0), "heart" to (reactions["❤️"] ?: 0), "music" to (reactions["🎵"] ?: 0), "title" to soundtrackTitle)
                    if (currentUserId.isNotBlank()) updates["user_$currentUserId"] = newReacted ?: ""
                    docRef.set(updates, com.google.firebase.firestore.SetOptions.merge())
                } catch (e: Exception) { }
            }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(emoji, fontSize = 14.sp)
                    if (count > 0) Text("$count", fontFamily = BangersFontFamily, color = if (isReacted) CGreenDeep else ScrapbookDark.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
            LaunchedEffect(popped) { if (popped) { delay(200); popped = false } }
        }
    }
}
// ─── My Retro Era ────────────────────────────────────────────────────────────

@Composable
fun RetroEraSection(userId: String) {
    val eras = listOf(
        "NES Era" to "🕹️", "SNES Era" to "🎮", "PS1 / N64" to "💾",
        "PS2 / GC" to "💿", "PS3 / 360" to "📀", "PS4 / One" to "🎯", "Modern" to "🚀"
    )
    var selectedEra by remember { mutableStateOf("") }
    LaunchedEffect(userId) {
        if (userId.isBlank()) return@LaunchedEffect
        try {
            val doc = FirebaseFirestore.getInstance().collection("users").document(userId).get().await()
            selectedEra = doc.getString("retroEra") ?: ""
        } catch (e: Exception) { }
    }
    val neonAlpha by rememberGlowRange(0.4f, 1f)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().offset(x = 3.dp, y = 3.dp).clip(RoundedCornerShape(14.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(ComicGlassBg, ComicGlassBg)))
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.width(3.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(neonAlpha)))
                        Text("MY RETRO ERA", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, letterSpacing = 2.sp)
                        Text("peak gaming years", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // 2-row wrap of era chips
                    val rows = eras.chunked(4)
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                            row.forEach { eraPair ->
                                val eraName = eraPair.first; val emoji = eraPair.second
                                val isSelected = selectedEra == eraName
                                var pressed by remember { mutableStateOf(false) }
                                val scale by animateFloatAsState(targetValue = if (pressed) 0.91f else 1f, animationSpec = spring(Spring.DampingRatioMediumBouncy), label = "eraScale_$eraName")
                                Box(modifier = Modifier.scale(scale)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) CGreen else Color.Transparent)
                                    .border(1.5.dp, if (isSelected) CGreenDeep else CGreen.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                    .clickable {
                                        pressed = true
                                        val newEra = if (selectedEra == eraName) "" else eraName
                                        selectedEra = newEra
                                        if (userId.isNotBlank()) {
                                            FirebaseFirestore.getInstance().collection("users").document(userId).update("retroEra", newEra)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(emoji, fontSize = 11.sp)
                                        Text(eraName, fontFamily = BangersFontFamily, color = if (isSelected) ScrapbookDark else ScrapbookDark, fontSize = 10.sp)
                                    }
                                }
                                LaunchedEffect(pressed) { if (pressed) { delay(160); pressed = false } }
                            }
                        }
                    }
                    if (selectedEra.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val selectedPair = eras.firstOrNull { it.first == selectedEra }
                        Text("${selectedPair?.second ?: ""} Your peak era: $selectedEra", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ─── Backlog Stats ────────────────────────────────────────────────────────────

@Composable
fun BacklogStatsSection(userId: String) {
    var playing by remember { mutableStateOf(0) }
    var finished by remember { mutableStateOf(0) }
    var backlog by remember { mutableStateOf(0) }
    LaunchedEffect(userId) {
        if (userId.isBlank()) return@LaunchedEffect
        try {
            val doc = FirebaseFirestore.getInstance().collection("users").document(userId).get().await()
            playing = (doc.getLong("backlogPlaying") ?: 0L).toInt()
            finished = (doc.getLong("backlogFinished") ?: 0L).toInt()
            backlog = (doc.getLong("backlogWant") ?: 0L).toInt()
        } catch (e: Exception) { }
    }
    fun save() {
        if (userId.isBlank()) return
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .update(mapOf("backlogPlaying" to playing, "backlogFinished" to finished, "backlogWant" to backlog))
    }
    val neonAlpha by rememberGlowRange(0.4f, 1f)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
            Box(modifier = Modifier.width(3.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(neonAlpha)))
            Text("GAME BACKLOG", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, letterSpacing = 2.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(
                Triple("▶ PLAYING", playing, CGreen),
                Triple("✓ FINISHED", finished, CGreen),
                Triple("📋 BACKLOG", backlog, Color(0xFF42A5F5))
            ).forEach { triple ->
                val label = triple.first; var value = triple.second; val color = triple.third
                Box(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.fillMaxWidth().offset(x = 2.dp, y = 2.dp).clip(RoundedCornerShape(12.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.92f))
                        .border(2.5.dp, ScrapbookDark, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(label, fontFamily = BangersFontFamily, color = color, fontSize = 9.sp, letterSpacing = 0.5.sp, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("$value", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 28.sp, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.15f)).border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp)).clickable {
                                    if (value > 0) { value--
                                        if (label.contains("PLAYING")) { playing = value } else if (label.contains("FINISHED")) { finished = value } else { backlog = value }
                                        save()
                                    }
                                }, contentAlignment = Alignment.Center) { Text("−", fontFamily = BangersFontFamily, color = color, fontSize = 14.sp) }
                                Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.15f)).border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp)).clickable {
                                    value++
                                    if (label.contains("PLAYING")) { playing = value } else if (label.contains("FINISHED")) { finished = value } else { backlog = value }
                                    save()
                                }, contentAlignment = Alignment.Center) { Text("+", fontFamily = BangersFontFamily, color = color, fontSize = 14.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SoundtrackDetailBottomSheet(soundtrack: Soundtrack, index: Int, currentUserId: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var igdbData by remember { mutableStateOf<IGDBGame?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(soundtrack.title) { try { val query = soundtrack.artist ?: soundtrack.title.replace(" OST", ""); val results = IGDBRepository.searchGames(query); igdbData = results.firstOrNull() } catch (e: Exception) { }; isLoading = false }
    val neonAlpha by rememberGlowRange(0.4f, 1f)
    val spinT = rememberInfiniteTransition(label = "stSheetSpin")
    val spinAngle by spinT.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart), label = "stSheetSpinAngle")

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)).clickable { onDismiss() }, contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.82f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Brush.verticalGradient(colors = listOf(ComicGlassBg, ComicGlassBg)))
                .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clickable { }
        ) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp).width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookDark.copy(alpha = 0.3f)))
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(220.dp).clip(CircleShape).background(Brush.radialGradient(colors = listOf(CGreen.copy(alpha = neonAlpha * 0.25f), Color.Transparent))))
                    Box(
                        modifier = Modifier.size(200.dp).graphicsLayer { rotationZ = spinAngle }.clip(CircleShape).background(ScrapbookDark),
                        contentAlignment = Alignment.Center
                    ) {
                        listOf(0.98f, 0.88f, 0.78f, 0.68f, 0.58f).forEach { f ->
                            Box(modifier = Modifier.size((200 * f).dp).clip(CircleShape).border(0.5.dp, Color.White.copy(alpha = 0.05f), CircleShape))
                        }
                        when {
                            soundtrack.coverUrl != null -> AsyncImage(model = soundtrack.coverUrl, contentDescription = soundtrack.title, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(soundtrack.coverUrl).size(200.dp).clip(CircleShape))
                            soundtrack.imageResId != null -> Image(painterResource(soundtrack.imageResId), soundtrack.title, modifier = Modifier.size(200.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                        }
                        Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.Black.copy(alpha = 0.25f)))
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(ScrapbookDark).border(2.dp, CGreen.copy(alpha = 0.4f), CircleShape))
                    }
                    Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = (-10).dp, y = 20.dp).width(3.dp).height(90.dp).graphicsLayer { rotationZ = -25f }.background(Brush.verticalGradient(colors = listOf(ScrapbookDark.copy(alpha = 0.7f), ScrapbookDark.copy(alpha = 0.3f)))))
                    Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = (-6).dp, y = 105.dp).size(8.dp).clip(CircleShape).background(CGreen))
                }
                Spacer(modifier = Modifier.height(20.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val dotT = rememberInfiniteTransition(label = "nowPlayingDot")
                        val dotAlpha by dotT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(600, easing = EaseInOut), RepeatMode.Reverse), label = "dotAlpha")
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CGreen.copy(alpha = dotAlpha)))
                        Text("NOW PLAYING", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 13.sp, letterSpacing = 2.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                            repeat(5) { i ->
                                val eqT = rememberInfiniteTransition(label = "eq_$i")
                                val eqH by eqT.animateFloat(initialValue = 4f, targetValue = 16f, animationSpec = infiniteRepeatable(tween(300 + i * 80, easing = EaseInOut), RepeatMode.Reverse), label = "eqBar_$i")
                                Box(modifier = Modifier.width(3.dp).height(eqH.dp).clip(RoundedCornerShape(1.dp)).background(CGreen.copy(alpha = 0.8f)))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(soundtrack.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 26.sp, lineHeight = 30.sp)
                    soundtrack.artist?.let { Text(it, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 14.sp) }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) { ThreeDotsAnimation() }
                    } else {
                        igdbData?.let { data ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                data.rating?.let { rating ->
                                    Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(CGreen.copy(alpha = 0.1f)).border(1.dp, CGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("${rating.toInt()}", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 26.sp)
                                            Text("RATING", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.55f), fontSize = 9.sp, letterSpacing = 1.sp)
                                        }
                                    }
                                }
                                data.releaseYear?.let { year ->
                                    Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.92f)).border(1.dp, ScrapbookDark.copy(alpha = 0.3f), RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("$year", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 26.sp)
                                            Text("RELEASED", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.55f), fontSize = 9.sp, letterSpacing = 1.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("REACTIONS", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 11.sp, letterSpacing = 1.sp)
                        SoundtrackReactionBar(soundtrackTitle = soundtrack.title, currentUserId = currentUserId)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(CGreen.copy(alpha = 0.12f))
                            .border(1.5.dp, CGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .clickable {
                                val query = "${soundtrack.title} game soundtrack".replace(" ", "+")
                                val url = "https://archive.org/search?query=$query"
                                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) { }
                            }
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🎵", fontSize = 16.sp)
                            Text("LISTEN ON ARCHIVE.ORG →", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 14.sp, letterSpacing = 1.sp)
                        }
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

    val spinT = rememberInfiniteTransition(label = "vinyl_$index")
    val spinAngle by spinT.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(if (isPlaying) 3000 else 8000, easing = LinearEasing), RepeatMode.Restart), label = "vinylAngle_$index")
    val glowAlpha by rememberGlowRange(0.2f, 0.7f)
    val neonAlpha by rememberGlowRange(0.3f, 0.8f)
    val flipRotation by animateFloatAsState(targetValue = if (isFlipped) 180f else 0f, animationSpec = tween(500, easing = EaseInOut), label = "flip_$index")
    var visible by remember { mutableStateOf(false) }
    val enterOffset by animateFloatAsState(targetValue = if (visible) 0f else 30f, animationSpec = tween(350, delayMillis = index * 60, easing = LinearOutSlowInEasing), label = "stEnter_$index")
    val enterAlpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(300, delayMillis = index * 60), label = "stEnterAlpha_$index")
    LaunchedEffect(Unit) { visible = true }

    Column(modifier = modifier.width(160.dp).offset(y = enterOffset.dp).graphicsLayer { alpha = enterAlpha }.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.height(28.dp), contentAlignment = Alignment.Center) {
            if (index == 0) {
                Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(ScrapbookDark).border(1.dp, CGreen.copy(alpha = neonAlpha), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val dotT = rememberInfiniteTransition(label = "nowDot_$index")
                    val dotA by dotT.animateFloat(initialValue = 0.3f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(500, easing = EaseInOut), RepeatMode.Reverse), label = "nowDotAlpha_$index")
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(CGreen.copy(alpha = dotA)))
                    Text("NOW PLAYING", fontFamily = BangersFontFamily, color = CGreen, fontSize = 9.sp, letterSpacing = 1.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp), verticalAlignment = Alignment.Bottom) {
                        repeat(4) { i ->
                            val eqT = rememberInfiniteTransition(label = "smallEq_${index}_$i")
                            val eqH by eqT.animateFloat(initialValue = 3f, targetValue = 10f, animationSpec = infiniteRepeatable(tween(280 + i * 70, easing = EaseInOut), RepeatMode.Reverse), label = "smallEqH_${index}_$i")
                            Box(modifier = Modifier.width(2.dp).height(eqH.dp).clip(RoundedCornerShape(1.dp)).background(CGreen.copy(alpha = 0.8f)))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .graphicsLayer { rotationY = flipRotation }
                .clickable {
                    val now = System.currentTimeMillis()
                    if (now - lastTapTime < 400) showDetail = true else isPlaying = !isPlaying
                    lastTapTime = now
                }
                .pointerInput(Unit) { detectTapGestures(onLongPress = { isFlipped = !isFlipped }) }
        ) {
            Box(modifier = Modifier.size(155.dp).clip(CircleShape).background(if (isPlaying) CGreen.copy(alpha = glowAlpha * 0.5f) else Color.Transparent))

            Box(modifier = Modifier.size(148.dp).graphicsLayer { rotationZ = spinAngle }.clip(CircleShape), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(ScrapbookDark))
                when {
                    soundtrack.coverUrl != null -> AsyncImage(model = soundtrack.coverUrl, contentDescription = soundtrack.title, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(soundtrack.coverUrl).size(148.dp).clip(CircleShape))
                    soundtrack.imageResId != null -> Image(painterResource(soundtrack.imageResId), soundtrack.title, modifier = Modifier.size(148.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                }
                Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Brush.radialGradient(colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.45f)))))
                listOf(0.95f, 0.82f, 0.68f, 0.54f).forEach { f ->
                    Box(modifier = Modifier.size((148 * f).dp).clip(CircleShape).border(0.5.dp, Color.Black.copy(alpha = 0.35f), CircleShape))
                }
                Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0f)))))
                Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(ScrapbookDark).border(1.5.dp, Color.White.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                    Text(soundtrack.title.take(2).uppercase(), fontFamily = BangersFontFamily, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                }
            }

            if (isPlaying) {
                Box(modifier = Modifier.size(152.dp).clip(CircleShape).border(width = 2.5.dp, brush = Brush.sweepGradient(colors = listOf(CGreen.copy(alpha = 0f), CGreen.copy(alpha = neonAlpha), CGreen.copy(alpha = neonAlpha * 0.6f), CGreen.copy(alpha = 0f))), shape = CircleShape))
            }
        }

        if (isFlipped) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookDark, RoundedCornerShape(10.dp)).padding(10.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("TRACK INFO", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 11.sp, letterSpacing = 1.sp)
                    Text(soundtrack.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                    soundtrack.artist?.let { Text("By $it", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp) }
                    Text("▶ Tap = play/stop\n👆 Double tap = details\n📱 Long press = flip", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.5f), fontSize = 9.sp, lineHeight = 14.sp)
                }
            }
        }

        if (!isFlipped) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(soundtrack.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
            soundtrack.artist?.let { Text(it, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth()) }
            if (isPlaying) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(CGreen.copy(alpha = 0.15f)).border(1.dp, CGreen.copy(alpha = 0.4f), RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text("▶ PLAYING", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 9.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("TAP PLAY · DOUBLE-TAP DETAILS · LONG PRESS FLIP", fontFamily = BangersFontFamily, color = ScrapbookTextMuted.copy(alpha = 0.4f), fontSize = 7.sp, letterSpacing = 0.5.sp, textAlign = TextAlign.Center)
        }
    }

    if (showDetail) { SoundtrackDetailBottomSheet(soundtrack = soundtrack, index = index, currentUserId = currentUserId, onDismiss = { showDetail = false }) }
}

// ─── Top Soundtracks Section ──────────────────────────────────────────────────

@Composable
fun TopSoundtracksSection(soundtracks: List<Soundtrack>, currentUserId: String = "") {
    if (soundtracks.isEmpty()) return
    val neonAlpha by rememberGlowRange(0.3f, 0.8f)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 4.dp), horizontalArrangement = Arrangement.End) {
            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.6f)).border(1.dp, CGreen.copy(alpha = neonAlpha * 0.4f), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text("👆 TAP  •  2x SPEED  •  LONG PRESS FLIP", fontFamily = BangersFontFamily, color = ScrapbookTextMuted, fontSize = 8.sp, letterSpacing = 0.5.sp)
            }
        }
        LazyRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Bottom) {
            items(soundtracks.take(3).size) { index ->
                SoundtrackItem(soundtrack = soundtracks[index], index = index, currentUserId = currentUserId)
            }
        }
    }
}

// ─── Genre Slideshow + Radar Chart ───────────────────────────────────────────

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
        for (name in gameNames) {
            try {
                val result = IGDBRepository.fetchGameCoverByName(name)
                if (result != null) covers.add(result)
                if (covers.size >= 5) break
            } catch (e: Exception) { }
        }
        slideCovers = covers
    }
    LaunchedEffect(slideCovers) {
        if (slideCovers.size > 1) {
            while (true) {
                delay(3500L); slideVisible = false
                delay(400L); currentSlide = (currentSlide + 1) % slideCovers.size
                slideVisible = true
            }
        }
    }

    val neonAlpha by rememberGlowRange(0.4f, 1f)
    val slideAlpha by animateFloatAsState(targetValue = if (slideVisible) 1f else 0f, animationSpec = tween(400, easing = EaseInOut), label = "slideAlpha")
    val genreAccentColor = when (topGenre) {
        "RPG" -> CAcPurple; "Action" -> CAcRed; "Platformer" -> CAcBlue
        "Shooter" -> CGreenDeep; "Adventure" -> CGreenDeep; "Arcade" -> CAcYellow
        else -> CGreen
    }

    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = Color.White.copy(alpha = 0.46f), cornerRadius = 14.dp, shadowOffset = 4.dp) {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(14.dp))
            ) {
                if (slideCovers.isNotEmpty()) {
                    val coverUrl = slideCovers.getOrNull(currentSlide)
                    if (coverUrl != null) {
                        AsyncImage(model = coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(coverUrl).fillMaxWidth().height(400.dp).graphicsLayer { alpha = slideAlpha * 0.35f })
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(400.dp).background(Brush.verticalGradient(colors = listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.85f), ComicGlassBg))))
                }
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎯 GAMING PERSONALITY", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 20.sp, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Based on your top games", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 12.sp)
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(genreAccentColor.copy(alpha = 0.8f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text("▶ $topGenre", fontFamily = BangersFontFamily, color = Color.White, fontSize = 11.sp)
                        }
                    }
                    if (slideCovers.size > 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            slideCovers.forEachIndexed { i, _ ->
                                Box(modifier = Modifier.size(if (i == currentSlide) 8.dp else 5.dp).clip(CircleShape).background(if (i == currentSlide) genreAccentColor else ScrapbookDark.copy(alpha = 0.25f)))
                            }
                        }
                    }
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
                            drawPath(rp, ScrapbookDark.copy(alpha = 0.15f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                        }
                        for (i in 0 until count) {
                            val angle = i * angleStep - (Math.PI / 2).toFloat()
                            drawLine(ScrapbookDark.copy(alpha = 0.2f), center, androidx.compose.ui.geometry.Offset(center.x + radius * kotlin.math.cos(angle), center.y + radius * kotlin.math.sin(angle)), 1.dp.toPx())
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
                        drawPath(rp, genreAccentColor.copy(alpha = 0.3f))
                        drawPath(rp, genreAccentColor, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                        genres.forEachIndexed { i, genre ->
                            val angle = i * angleStep - (Math.PI / 2).toFloat()
                            val r = radius * genre.score * animProgress
                            val cx = center.x + r * kotlin.math.cos(angle)
                            val cy = center.y + r * kotlin.math.sin(angle)
                            drawCircle(genreAccentColor, 5.dp.toPx(), androidx.compose.ui.geometry.Offset(cx, cy))
                            drawCircle(Color.White, 2.5f.dp.toPx(), androidx.compose.ui.geometry.Offset(cx, cy))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    genres.chunked(3).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { genre ->
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (genre.genre == topGenre) genreAccentColor else CGreen.copy(alpha = 0.5f)))
                                    Text(genre.genre, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = if (genre.genre == topGenre) genreAccentColor else ScrapbookDark.copy(alpha = 0.8f), fontSize = 11.sp)
                                    Text("${(genre.score * 100).toInt()}%", fontFamily = NunitoFontFamily, color = if (genre.genre == topGenre) genreAccentColor.copy(alpha = 0.8f) else ScrapbookDark.copy(alpha = 0.5f), fontSize = 10.sp)
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

@Preview(showBackground = true, backgroundColor = 0xFFFAF3E0)
// ─── AnimatedStatCard ─────────────────────────────────────────────────────────
@Composable
fun AnimatedStatCard(value: String, label: String, neonAlpha: Float = 0.6f, onClick: () -> Unit) {
    val glowAlpha by rememberGlowPhase(0.5f)
    var pressed by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    val pressAnim by animateFloatAsState(
        targetValue = if (pressed) 3f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "ascPress_$label"
    )
    val shadowOffset by animateFloatAsState(
        targetValue = if (pressed) 0f else 3f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ascShadowOff_$label"
    )
    val entryAlpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(600), label = "asca_$label")
    val entryOffset by animateFloatAsState(targetValue = if (visible) 0f else 18f, animationSpec = tween(500, easing = FastOutSlowInEasing), label = "asco_$label")
    val tiltDeg = remember(label) { ((label.hashCode() % 5) - 2) * 0.5f }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(150); visible = true }
    Box(modifier = Modifier
        .width(82.dp)
        .graphicsLayer { alpha = entryAlpha; translationY = entryOffset; rotationZ = tiltDeg }
        .clickable { pressed = true; onClick() }
    ) {
        // Outer glow halo
        Box(modifier = Modifier.matchParentSize()
            .offset(x = 6.dp, y = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CGreen.copy(alpha = if (pressed) 0f else glowAlpha * 0.30f)))
        // Hard green shadow
        Box(modifier = Modifier.matchParentSize()
            .offset(x = shadowOffset.dp, y = shadowOffset.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CGreen.copy(alpha = if (pressed) 0.25f else 1f)))
        // Glass card (shifts on press)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = pressAnim.dp, y = pressAnim.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            // Dot halftone texture
            HalftoneDots(Modifier.matchParentSize(), spacing = 6.dp, dotRadius = 1.2.dp, color = Color.Black.copy(alpha = 0.12f))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                RollingCounterText(value, androidx.compose.ui.text.TextStyle(fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp))
                Spacer(modifier = Modifier.height(2.dp))
                Text(label, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 9.sp, letterSpacing = 0.5.sp, textAlign = TextAlign.Center)
            }
            ComicShimmer(Modifier.matchParentSize(), cornerRadius = 14.dp)
        }
    }
    LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(150); pressed = false } }
}

// ─── StreakStatCard ───────────────────────────────────────────────────────────
@Composable
fun StreakStatCard(streak: Int, total: Int, neonAlpha: Float) {
    val fireT = rememberInfiniteTransition(label = "streakFire")
    val fireScale by fireT.animateFloat(initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(600, easing = EaseInOut), RepeatMode.Reverse), label = "fireS")
    val glowAlpha by rememberGlowPhase(0.5f)
    var pressed by remember { mutableStateOf(false) }
    val pressAnim by animateFloatAsState(
        targetValue = if (pressed) 3f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "streakPressAnim"
    )
    val shadowOffset by animateFloatAsState(
        targetValue = if (pressed) 0f else 3f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "streakShadowOff"
    )
    Box(modifier = Modifier.width(90.dp).graphicsLayer { rotationZ = 0.6f }.clickable { pressed = true }) {
        // Outer glow halo
        Box(modifier = Modifier.matchParentSize().offset(x = 6.dp, y = 6.dp).clip(RoundedCornerShape(14.dp))
            .background(CGreen.copy(alpha = if (pressed) 0f else glowAlpha * 0.30f)))
        // Hard green shadow
        Box(modifier = Modifier.matchParentSize().offset(x = shadowOffset.dp, y = shadowOffset.dp).clip(RoundedCornerShape(14.dp))
            .background(CGreen.copy(alpha = if (pressed) 0.25f else 1f)))
        // Glass card
        Box(modifier = Modifier.fillMaxWidth()
            .offset(x = pressAnim.dp, y = pressAnim.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            // Dot halftone texture
            HalftoneDots(Modifier.matchParentSize(), spacing = 6.dp, dotRadius = 1.2.dp, color = Color.Black.copy(alpha = 0.12f))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Flame grows with the streak and flickers
                    val flicker by rememberGlowRange(-6f, 6f)
                    Text("🔥", fontSize = (14 + streak.coerceAtMost(10)).sp,
                        modifier = Modifier.graphicsLayer {
                            scaleX = fireScale; scaleY = fireScale * (1f + streak.coerceAtMost(10) * 0.015f)
                            rotationZ = flicker
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                        })
                    RollingCounterText("$streak", androidx.compose.ui.text.TextStyle(fontFamily = BangersFontFamily, color = Color(0xFFCC4400), fontSize = 20.sp))
                }
                Text("DAY STREAK", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 9.sp, letterSpacing = 0.5.sp)
                if (total > 0) Text("$total POSTS", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted.copy(0.6f), fontSize = 8.sp)
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(150); pressed = false } }
}

// ─── NeonSectionHeader — RetroLabel D style ───────────────────────────────────
@Composable
fun NeonSectionHeader(title: String, emoji: String, neonAlpha: Float) {
    val glowAlpha by rememberGlowPhase(0.45f)
    var pressed by remember { mutableStateOf(false) }
    val pressAnim by animateFloatAsState(
        targetValue = if (pressed) 4f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "seclblPress"
    )
    val shadowOffset by animateFloatAsState(
        targetValue = if (pressed) 0f else 4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "seclblShadowOff"
    )
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        // Outer glow halo (pulsing)
        Box(modifier = Modifier.matchParentSize()
            .offset(x = 7.dp, y = 7.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CGreen.copy(alpha = if (pressed) 0f else glowAlpha * 0.28f)))
        // Hard green shadow (collapses on press)
        Box(modifier = Modifier.matchParentSize()
            .offset(x = shadowOffset.dp, y = shadowOffset.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CGreen.copy(alpha = if (pressed) 0.3f else 1f)))
        // Glass label
        Box(modifier = Modifier.fillMaxWidth()
            .offset(x = pressAnim.dp, y = pressAnim.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(8.dp))
            .clickable { pressed = true }
            .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            // Dot halftone texture
            HalftoneDots(Modifier.matchParentSize(), spacing = 6.dp, dotRadius = 1.3.dp, color = Color.Black.copy(alpha = 0.13f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(emoji, fontSize = 18.sp)
                Text(title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

@Composable
fun ProfileScreenPreview() { HubRetroTheme { ProfileScreen() } }
package com.example.hubretro

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

// ─── Data Classes ────────────────────────────────────────────────────────────

data class MagazineCover(
    val id: String,
    val title: String,
    val coverImageResId: Int? = null,
    val coverImageUrl: String? = null,
    val webUrl: String? = null,
    val platform: String = "",      // ✅ for "Because You Read" recommendations
    val era: String = ""            // ✅ e.g. "SNES", "PS1", "ARCADE"
)

data class ReadingProgress(
    val magazineId: String = "",
    val magazineTitle: String = "",
    val magazineCoverUrl: String = "",
    val lastReadAt: Long = 0L,
    val percentComplete: Float = 0f
)

data class MagazineShelf(
    val id: String = "",
    val name: String = "",
    val emoji: String = "📚",
    val magazineIds: List<String> = emptyList(),
    val createdAt: Long = 0L
)

data class MagazineRating(
    val magazineId: String = "",
    val userId: String = "",
    val stars: Int = 0,
    val review: String = "",
    val username: String = "",
    val timestamp: Long = 0L
)

data class ReadingChallenge(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val type: String, // "MONTHLY" or "THEME"
    val target: Int,
    val current: Int = 0,
    val badgeId: String = "",
    val isComplete: Boolean = false
)

data class MagazineComment(
    val id: String = "",
    val magazineId: String = "",
    val userId: String = "",
    val username: String = "",
    val profilePicUrl: String = "",
    val text: String = "",
    val page: Int? = null,          // null = magazine-level, number = page-level
    val timestamp: Long = 0L
)

// ─── Sample Data ─────────────────────────────────────────────────────────────

val uniqueCoverResourceIds = listOf(
    R.drawable.cover1, R.drawable.cover2, R.drawable.cover3,
    R.drawable.cover4, R.drawable.cover5, R.drawable.cover6,
    R.drawable.cover7, R.drawable.cover8, R.drawable.cover9
)

val sampleMagazineCovers = listOf(
    MagazineCover("1", "GamePro #130", coverImageResId = R.drawable.cover1, platform = "PS1", era = "PS1"),
    MagazineCover("2", "EGM #89", coverImageResId = R.drawable.cover2, platform = "SNES", era = "SNES"),
    MagazineCover("3", "Nintendo Power #55", coverImageResId = R.drawable.cover3, platform = "SNES", era = "SNES"),
    MagazineCover("4", "GameFan Vol 3", coverImageResId = R.drawable.cover4, platform = "SEGA", era = "SEGA"),
    MagazineCover("5", "Retro Gamer #12", coverImageResId = R.drawable.cover5, platform = "NES", era = "NES"),
    MagazineCover("6", "Computer Gaming World", coverImageResId = R.drawable.cover6, platform = "PC", era = "PC"),
    MagazineCover("7", "GameFan Vol 7", coverImageResId = R.drawable.cover7, platform = "PS1", era = "PS1"),
    MagazineCover("8", "EGM Special Edition", coverImageResId = R.drawable.cover8, platform = "N64", era = "N64"),
    MagazineCover("9", "Nintendo Power #77", coverImageResId = R.drawable.cover9, platform = "SNES", era = "SNES"),
)

// ─── Hardcoded Challenges ─────────────────────────────────────────────────────

val allChallenges = listOf(
    ReadingChallenge("c1", "Monthly Reader", "Read 3 magazines this month", "📅", "MONTHLY", 3),
    ReadingChallenge("c2", "Binge Reader", "Read 5 magazines this month", "🔥", "MONTHLY", 5),
    ReadingChallenge("c3", "SNES Scholar", "Read 3 SNES-era magazines", "🎮", "THEME", 3),
    ReadingChallenge("c4", "PS1 Pioneer", "Read 3 PS1-era magazines", "🎯", "THEME", 3),
    ReadingChallenge("c5", "NES Historian", "Read all NES-era magazines", "🕹️", "THEME", 2),
    ReadingChallenge("c6", "Collector", "Add 5 magazines to a shelf", "🗂️", "THEME", 5),
    ReadingChallenge("c7", "Critic", "Rate 3 different magazines", "⭐", "THEME", 3),
    ReadingChallenge("c8", "Completionist", "Read 10 magazines total", "🏆", "MONTHLY", 10),
)

fun ArchiveItem.toMagazineCover() = MagazineCover(
    id = this.id,
    title = this.title,
    coverImageResId = null,
    coverImageUrl = this.thumbnailUrl,
    webUrl = this.webUrl
)

fun toArchiveEmbedUrl(webUrl: String): String = webUrl

fun injectHideStyles(view: WebView?) {
    view?.evaluateJavascript(
        """
        (function() {
            var style = document.createElement('style');
            style.innerHTML = `
                #oc-hdr, #nav-tophat, .topinblock, header {
                    display: none !important;
                }
                body { margin-top: 0 !important; padding-top: 0 !important; }
            `;
            document.head.appendChild(style);
            try {
                var br = document.querySelector('#bookreader, .BookReader, #BookReader');
                if (br) { br.scrollIntoView({ behavior: 'smooth' }); }
            } catch(e) {}
        })();
        """.trimIndent(), null
    )
}

// ─── Magazine Reader ──────────────────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MagazineReaderScreen(
    url: String,
    title: String,
    magazineId: String,
    onClose: () -> Unit,
    onProgressUpdate: (Float) -> Unit = {}
) {
    val context = LocalContext.current
    val embedUrl = remember(url) { toArchiveEmbedUrl(url) }
    var isLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf(embedUrl) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showControls by remember { mutableStateOf(false) }
    var showComments by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showControls = true
        delay(3000L)
        showControls = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        setSupportZoom(true)
                        mediaPlaybackRequiresUserGesture = false
                        allowFileAccess = true
                        allowContentAccess = true
                        userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                "Chrome/112.0.0.0 Mobile Safari/537.36"
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                            url?.let { currentUrl = it }
                            injectHideStyles(view)
                            view?.postDelayed({ injectHideStyles(view) }, 1500)
                            view?.postDelayed({ injectHideStyles(view) }, 3000)
                        }
                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            isLoading = true
                        }
                    }
                    webChromeClient = WebChromeClient()
                    loadUrl(embedUrl)
                    webViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                color = ScrapbookYellow,
                trackColor = Color.Transparent
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    showControls = !showControls
                }
        )

        // Top controls
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically(tween(400, easing = LinearOutSlowInEasing)) { -it } + fadeIn(tween(400)),
            exit = slideOutVertically(tween(300, easing = FastOutLinearInEasing)) { -it } + fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.92f), Color.Black.copy(alpha = 0.0f))
                        )
                    )
                    .padding(top = 40.dp, bottom = 32.dp, start = 4.dp, end = 4.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Close", tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, fontFamily = BangersFontFamily, color = Color.White, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = "Tap anywhere to show/hide controls", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                    }
                    // ✅ Comments button
                    IconButton(onClick = { showComments = true }) {
                        Icon(Icons.Filled.Comment, contentDescription = "Comments", tint = ScrapbookYellow, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { webViewRef?.reload() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                        try { context.startActivity(intent) } catch (e: Exception) { }
                    }) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = "Open in browser", tint = ScrapbookYellow, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // ✅ Comments panel
        if (showComments) {
            MagazineCommentsPanel(
                magazineId = magazineId,
                onDismiss = { showComments = false }
            )
        }
    }
}

// ─── Feature 1: Continue Reading Strip ───────────────────────────────────────

@Composable
fun ContinueReadingStrip(
    progressList: List<ReadingProgress>,
    onResume: (ReadingProgress) -> Unit
) {
    if (progressList.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("▶", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "CONTINUE READING",
                fontFamily = BangersFontFamily,
                color = ScrapbookDark,
                fontSize = 20.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f)
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(progressList.take(5), key = { it.magazineId }) { progress ->
                ContinueReadingCard(progress = progress, onResume = { onResume(progress) })
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.15f))
    }
}

@Composable
fun ContinueReadingCard(progress: ReadingProgress, onResume: () -> Unit) {
    Box(modifier = Modifier.width(140.dp)) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth().clickable { onResume() },
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 10.dp,
            shadowOffset = 3.dp
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                        .background(ScrapbookPaper),
                    contentAlignment = Alignment.Center
                ) {
                    if (progress.magazineCoverUrl.isNotBlank()) {
                        AsyncImage(model = progress.magazineCoverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Text("📖", fontSize = 32.sp)
                    }
                    // Progress bar at bottom of image
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(ScrapbookDark.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.percentComplete)
                                .fillMaxHeight()
                                .background(ScrapbookYellow)
                        )
                    }
                }
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = progress.magazineTitle,
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${(progress.percentComplete * 100).toInt()}% read",
                        fontFamily = NunitoFontFamily,
                        color = ScrapbookTextMuted,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(ScrapbookDark)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("RESUME →", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ─── Feature 2: Collections / Bookshelves ────────────────────────────────────

@Composable
fun MyShelvesSection(
    shelves: List<MagazineShelf>,
    onCreateShelf: () -> Unit,
    onShelfTap: (MagazineShelf) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🗂️", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "MY SHELVES",
                fontFamily = BangersFontFamily,
                color = ScrapbookDark,
                fontSize = 20.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(ScrapbookDark)
                    .border(2.dp, ScrapbookBorder, CircleShape)
                    .clickable { onCreateShelf() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New shelf", tint = ScrapbookYellow, modifier = Modifier.size(18.dp))
            }
        }
        if (shelves.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookPaper, cornerRadius = 10.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📚", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No shelves yet — create one!", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(shelves, key = { it.id }) { shelf ->
                    ShelfCard(shelf = shelf, onTap = { onShelfTap(shelf) })
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.15f))
    }
}

@Composable
fun ShelfCard(shelf: MagazineShelf, onTap: () -> Unit) {
    Box(modifier = Modifier.width(120.dp)) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth().clickable { onTap() },
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 10.dp,
            shadowOffset = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(shelf.emoji, fontSize = 28.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = shelf.name, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, lineHeight = 17.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "${shelf.magazineIds.size} mag${if (shelf.magazineIds.size != 1) "s" else ""}", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun CreateShelfDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("📚") }
    val emojiOptions = listOf("📚", "🎮", "⭐", "🔥", "🏆", "🎯", "💾", "📺", "🕹️", "🌟")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ScrapbookCream,
        title = {
            Text("NEW SHELF", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Emoji picker
                Text("PICK AN EMOJI", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    emojiOptions.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (selectedEmoji == emoji) ScrapbookYellow else ScrapbookPaper)
                                .border(2.dp, ScrapbookBorder, CircleShape)
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 16.sp)
                        }
                    }
                }
                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Shelf name...", fontFamily = NunitoFontFamily, fontSize = 14.sp) },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextDark),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ScrapbookDark,
                        unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f),
                        focusedContainerColor = ScrapbookCardWhite,
                        unfocusedContainerColor = ScrapbookCardWhite,
                        cursorColor = ScrapbookDark
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (name.isNotBlank()) ScrapbookDark else ScrapbookDark.copy(alpha = 0.3f))
                    .clickable(enabled = name.isNotBlank()) { onConfirm(name.trim(), selectedEmoji) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("CREATE", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 16.sp)
            }
        },
        dismissButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ScrapbookPaper)
                    .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                    .clickable { onDismiss() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("CANCEL", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
            }
        }
    )
}

// ─── Feature 3: Ratings & Reviews ────────────────────────────────────────────

@Composable
fun StarRatingBar(
    rating: Int,
    onRate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = "$i stars",
                tint = if (i <= rating) ScrapbookYellowDark else ScrapbookDark.copy(alpha = 0.2f),
                modifier = Modifier.size(28.dp).clickable { onRate(i) }
            )
        }
    }
}

@Composable
fun MagazineRatingsSection(
    magazineId: String,
    currentUserId: String
) {
    var ratings by remember { mutableStateOf<List<MagazineRating>>(emptyList()) }
    var myRating by remember { mutableStateOf(0) }
    var myReview by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showReviewField by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(magazineId) {
        try {
            val docs = FirebaseFirestore.getInstance()
                .collection("magazine_ratings")
                .whereEqualTo("magazineId", magazineId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20).get().await()
            ratings = docs.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                MagazineRating(
                    magazineId = data["magazineId"] as? String ?: "",
                    userId = data["userId"] as? String ?: "",
                    stars = (data["stars"] as? Long)?.toInt() ?: 0,
                    review = data["review"] as? String ?: "",
                    username = data["username"] as? String ?: "",
                    timestamp = data["timestamp"] as? Long ?: 0L
                )
            }
            val mine = ratings.firstOrNull { it.userId == currentUserId }
            myRating = mine?.stars ?: 0
            myReview = mine?.review ?: ""
        } catch (e: Exception) { } finally { isLoading = false }
    }

    val avgRating = if (ratings.isEmpty()) 0f
    else ratings.map { it.stars }.average().toFloat()

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("⭐ RATINGS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp)
            if (ratings.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(String.format("%.1f", avgRating), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp)
                    Icon(Icons.Filled.Star, contentDescription = null, tint = ScrapbookYellowDark, modifier = Modifier.size(18.dp))
                    Text("(${ratings.size})", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // My rating
        if (currentUserId.isNotBlank()) {
            Box {
                ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookPaper, cornerRadius = 10.dp) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("YOUR RATING", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        StarRatingBar(rating = myRating, onRate = { star ->
                            myRating = star
                            showReviewField = true
                        })
                        if (showReviewField || myReview.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = myReview,
                                onValueChange = { myReview = it },
                                placeholder = { Text("Write a review...", fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookTextMuted) },
                                textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookTextDark),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ScrapbookDark,
                                    unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f),
                                    focusedContainerColor = ScrapbookCardWhite,
                                    unfocusedContainerColor = ScrapbookCardWhite,
                                    cursorColor = ScrapbookDark
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (myRating > 0) ScrapbookDark else ScrapbookDark.copy(alpha = 0.3f))
                                    .clickable(enabled = myRating > 0 && !isSubmitting) {
                                        isSubmitting = true
                                        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@clickable
                                        val ratingData = hashMapOf(
                                            "magazineId" to magazineId,
                                            "userId" to uid,
                                            "stars" to myRating,
                                            "review" to myReview,
                                            "username" to (FirebaseAuth.getInstance().currentUser?.displayName ?: ""),
                                            "timestamp" to System.currentTimeMillis()
                                        )
                                        FirebaseFirestore.getInstance()
                                            .collection("magazine_ratings")
                                            .document("${uid}_$magazineId")
                                            .set(ratingData)
                                            .addOnSuccessListener { isSubmitting = false; showReviewField = false }
                                            .addOnFailureListener { isSubmitting = false }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(color = ScrapbookYellow, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("SUBMIT RATING", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Other reviews
        if (!isLoading && ratings.isNotEmpty()) {
            Text("COMMUNITY REVIEWS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            ratings.filter { it.userId != currentUserId }.take(5).forEach { rating ->
                ReviewCard(rating = rating)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ReviewCard(rating: MagazineRating) {
    Box {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 10.dp, shadowOffset = 2.dp) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(rating.username.ifBlank { "Anonymous" }, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (i in 1..5) {
                            Icon(
                                imageVector = if (i <= rating.stars) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = null,
                                tint = if (i <= rating.stars) ScrapbookYellowDark else ScrapbookDark.copy(alpha = 0.15f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                if (rating.review.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(rating.review, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}

// ─── Feature 4: Reading Challenges ───────────────────────────────────────────

@Composable
fun ReadingChallengesSection(
    readCount: Int,
    ratingCount: Int,
    shelfItemCount: Int,
    readEras: List<String>
) {
    val challenges = remember(readCount, ratingCount, shelfItemCount, readEras) {
        allChallenges.map { challenge ->
            val current = when (challenge.id) {
                "c1", "c2", "c8" -> readCount
                "c3" -> readEras.count { it == "SNES" }
                "c4" -> readEras.count { it == "PS1" }
                "c5" -> readEras.count { it == "NES" }
                "c6" -> shelfItemCount
                "c7" -> ratingCount
                else -> 0
            }
            challenge.copy(current = current.coerceAtMost(challenge.target), isComplete = current >= challenge.target)
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text("🏆 READING CHALLENGES", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Complete challenges to earn badges!", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Text("MONTHLY", fontFamily = BangersFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(6.dp))
        challenges.filter { it.type == "MONTHLY" }.forEach { challenge ->
            ChallengeCard(challenge = challenge)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text("THEME", fontFamily = BangersFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(6.dp))
        challenges.filter { it.type == "THEME" }.forEach { challenge ->
            ChallengeCard(challenge = challenge)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ChallengeCard(challenge: ReadingChallenge) {
    Box {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = if (challenge.isComplete) ScrapbookYellow.copy(alpha = 0.2f) else ScrapbookCardWhite,
            borderColor = if (challenge.isComplete) ScrapbookYellowDark else ScrapbookBorder,
            cornerRadius = 10.dp,
            shadowOffset = 2.dp
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (challenge.isComplete) ScrapbookYellow else ScrapbookPaper)
                        .border(2.dp, ScrapbookBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (challenge.isComplete) "✅" else challenge.emoji, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(challenge.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                    Text(challenge.description, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, lineHeight = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(ScrapbookPaper)
                            .border(1.dp, ScrapbookBorder.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
                    ) {
                        val progress = if (challenge.target > 0)
                            challenge.current.toFloat() / challenge.target else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (challenge.isComplete) ScrapbookGreen else ScrapbookYellowDark)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${challenge.current}/${challenge.target}",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = if (challenge.isComplete) ScrapbookGreen else ScrapbookTextMuted,
                        fontSize = 11.sp
                    )
                }
                if (challenge.isComplete) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ScrapbookYellow)
                            .border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("DONE!", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ─── Feature 5: In-Magazine Comments ─────────────────────────────────────────

@Composable
fun MagazineCommentsPanel(
    magazineId: String,
    onDismiss: () -> Unit
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    var comments by remember { mutableStateOf<List<MagazineComment>>(emptyList()) }
    var newComment by remember { mutableStateOf("") }
    var selectedPage by remember { mutableStateOf<Int?>(null) }
    var pageInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(magazineId) {
        try {
            val docs = FirebaseFirestore.getInstance()
                .collection("magazine_comments")
                .whereEqualTo("magazineId", magazineId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50).get().await()
            comments = docs.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                MagazineComment(
                    id = doc.id,
                    magazineId = data["magazineId"] as? String ?: "",
                    userId = data["userId"] as? String ?: "",
                    username = data["username"] as? String ?: "",
                    profilePicUrl = data["profilePicUrl"] as? String ?: "",
                    text = data["text"] as? String ?: "",
                    page = (data["page"] as? Long)?.toInt(),
                    timestamp = data["timestamp"] as? Long ?: 0L
                )
            }.filter { it.text.isNotBlank() }
        } catch (e: Exception) { } finally { isLoading = false }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(ScrapbookCream)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ScrapbookYellow)
                        .border(BorderStroke(2.dp, ScrapbookBorder))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("💬 COMMENTS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = ScrapbookDark, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Filter tabs — ALL / by page
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(null to "ALL", null to "MAGAZINE").forEachIndexed { index, _ ->
                        val label = if (index == 0) "ALL" else "MAGAZINE-LEVEL"
                        val isSelected = if (index == 0) true else selectedPage == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (index == 0 && selectedPage == null) ScrapbookDark else if (index == 1 && selectedPage != null) ScrapbookDark else ScrapbookPaper)
                                .border(1.dp, ScrapbookBorder, RoundedCornerShape(20.dp))
                                .clickable { if (index == 1) selectedPage = null }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(label, fontFamily = BangersFontFamily, color = if (index == 0 && selectedPage == null) ScrapbookYellow else ScrapbookDark, fontSize = 12.sp)
                        }
                    }
                }

                // Comments list
                val displayComments = if (selectedPage == null) comments
                else comments.filter { it.page == selectedPage }

                if (isLoading) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ScrapbookYellowDark, modifier = Modifier.size(28.dp))
                    }
                } else if (displayComments.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💬", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No comments yet — be the first!", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayComments, key = { it.id }) { comment ->
                            CommentCard(comment = comment)
                        }
                    }
                }

                // Input area
                if (currentUser != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ScrapbookCardWhite)
                            .border(BorderStroke(1.dp, ScrapbookBorder.copy(alpha = 0.2f)))
                            .padding(12.dp)
                    ) {
                        // Optional page number
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Page #", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                            OutlinedTextField(
                                value = pageInput,
                                onValueChange = {
                                    pageInput = it
                                    selectedPage = it.toIntOrNull()
                                },
                                placeholder = { Text("optional", fontFamily = NunitoFontFamily, fontSize = 11.sp, color = ScrapbookTextMuted) },
                                singleLine = true,
                                textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 12.sp, color = ScrapbookTextDark),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ScrapbookDark,
                                    unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.2f),
                                    focusedContainerColor = ScrapbookCardWhite,
                                    unfocusedContainerColor = ScrapbookCardWhite,
                                    cursorColor = ScrapbookDark
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.width(80.dp)
                            )
                            if (pageInput.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ScrapbookYellow.copy(alpha = 0.3f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("PAGE $pageInput", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            OutlinedTextField(
                                value = newComment,
                                onValueChange = { newComment = it },
                                placeholder = { Text("Add a comment...", fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookTextMuted) },
                                textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookTextDark),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ScrapbookDark,
                                    unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f),
                                    focusedContainerColor = ScrapbookCardWhite,
                                    unfocusedContainerColor = ScrapbookCardWhite,
                                    cursorColor = ScrapbookDark
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                maxLines = 3
                            )
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (newComment.isNotBlank()) ScrapbookDark else ScrapbookDark.copy(alpha = 0.3f))
                                    .border(2.dp, ScrapbookBorder, CircleShape)
                                    .clickable(enabled = newComment.isNotBlank() && !isSubmitting) {
                                        isSubmitting = true
                                        val uid = currentUser.uid
                                        val commentData = hashMapOf(
                                            "magazineId" to magazineId,
                                            "userId" to uid,
                                            "username" to (currentUser.displayName ?: ""),
                                            "profilePicUrl" to (currentUser.photoUrl?.toString() ?: ""),
                                            "text" to newComment.trim(),
                                            "page" to pageInput.toIntOrNull(),
                                            "timestamp" to System.currentTimeMillis()
                                        )
                                        FirebaseFirestore.getInstance()
                                            .collection("magazine_comments")
                                            .add(commentData)
                                            .addOnSuccessListener { ref ->
                                                comments = listOf(
                                                    MagazineComment(
                                                        id = ref.id,
                                                        magazineId = magazineId,
                                                        userId = uid,
                                                        username = currentUser.displayName ?: "",
                                                        text = newComment.trim(),
                                                        page = pageInput.toIntOrNull(),
                                                        timestamp = System.currentTimeMillis()
                                                    )
                                                ) + comments
                                                newComment = ""
                                                pageInput = ""
                                                isSubmitting = false
                                            }
                                            .addOnFailureListener { isSubmitting = false }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(color = ScrapbookYellow, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Filled.Send, contentDescription = "Send", tint = ScrapbookYellow, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentCard(comment: MagazineComment) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(ScrapbookPaper)
                .border(2.dp, ScrapbookBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (comment.profilePicUrl.isNotBlank()) {
                AsyncImage(model = comment.profilePicUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(comment.username.take(1).uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(comment.username.ifBlank { "Anonymous" }, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                if (comment.page != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ScrapbookYellow.copy(alpha = 0.3f))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text("p.${comment.page}", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 10.sp)
                    }
                }
            }
            Text(comment.text, fontFamily = NunitoFontFamily, color = ScrapbookTextDark, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

// ─── Feature 6: Because You Read ─────────────────────────────────────────────

@Composable
fun BecauseYouReadSection(
    readHistory: List<String>,
    allMagazines: List<MagazineCover>,
    onMagazineTap: (MagazineCover) -> Unit
) {
    if (readHistory.isEmpty()) return

    val lastReadId = readHistory.lastOrNull() ?: return
    val lastRead = allMagazines.firstOrNull { it.id == lastReadId } ?: return

    val recommendations = remember(readHistory, allMagazines) {
        allMagazines
            .filter { it.id !in readHistory }
            .sortedByDescending { mag ->
                var score = 0
                if (mag.platform == lastRead.platform) score += 3
                if (mag.era == lastRead.era) score += 2
                score
            }
            .take(6)
    }

    if (recommendations.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.15f))
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("✨", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("BECAUSE YOU READ", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
                Text(lastRead.title, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(recommendations, key = { it.id }) { magazine ->
                Box(modifier = Modifier.width(80.dp)) {
                    MagazineCoverItem(
                        magazine = magazine,
                        onClick = { onMagazineTap(magazine) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

// ─── Updated MagazinesScreen ──────────────────────────────────────────────────

@Composable
fun MagazinesScreen(
    modifier: Modifier = Modifier,
    contentViewModel: ContentViewModel = viewModel(),
    favoritesViewModel: FavoritesViewModel? = null,
    authViewModel: AuthViewModel = viewModel()
) {
    val focusManager = LocalFocusManager.current
    val magazinesState by contentViewModel.magazinesState.collectAsState()
    val isLoadingMore by contentViewModel.isLoadingMoreMagazines.collectAsState()
    val hasMore by contentViewModel.hasMoreMagazines.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    val favoriteIds by (favoritesViewModel?.favoriteIds?.collectAsState()
        ?: remember { mutableStateOf(emptySet<String>()) })

    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var lastSearched by remember { mutableStateOf("") }
    var selectedMagazine by remember { mutableStateOf<MagazineCover?>(null) }
    var readerVisible by remember { mutableStateOf(false) }
    var visibleRows by remember { mutableStateOf(3) }

    // ✅ Feature 1 — Reading progress from Firestore
    var readingProgress by remember { mutableStateOf<List<ReadingProgress>>(emptyList()) }
    var readHistory by remember { mutableStateOf<List<String>>(emptyList()) }

    // ✅ Feature 2 — Shelves
    var shelves by remember { mutableStateOf<List<MagazineShelf>>(emptyList()) }
    var showCreateShelf by remember { mutableStateOf(false) }
    var addToShelfMagazine by remember { mutableStateOf<MagazineCover?>(null) }

    // ✅ Feature 3 — Ratings
    var showRatingsFor by remember { mutableStateOf<MagazineCover?>(null) }

    // ✅ Feature 4 — Challenges tracking
    var ratingCount by remember { mutableStateOf(0) }
    var shelfItemCount by remember { mutableStateOf(0) }

    // ✅ Active tab — BROWSE / SHELVES / CHALLENGES
    var activeTab by remember { mutableStateOf("BROWSE") }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            try {
                val db = FirebaseFirestore.getInstance()

                // Load reading progress
                val progressDocs = db.collection("users").document(uid)
                    .collection("reading_progress")
                    .orderBy("lastReadAt", Query.Direction.DESCENDING)
                    .limit(10).get().await()
                readingProgress = progressDocs.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    ReadingProgress(
                        magazineId = data["magazineId"] as? String ?: "",
                        magazineTitle = data["magazineTitle"] as? String ?: "",
                        magazineCoverUrl = data["magazineCoverUrl"] as? String ?: "",
                        lastReadAt = data["lastReadAt"] as? Long ?: 0L,
                        percentComplete = (data["percentComplete"] as? Double)?.toFloat() ?: 0f
                    )
                }
                readHistory = readingProgress.map { it.magazineId }

                // Load shelves
                val shelfDocs = db.collection("users").document(uid)
                    .collection("magazine_shelves")
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                    .get().await()
                shelves = shelfDocs.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    MagazineShelf(
                        id = doc.id,
                        name = data["name"] as? String ?: "",
                        emoji = data["emoji"] as? String ?: "📚",
                        magazineIds = (data["magazineIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        createdAt = data["createdAt"] as? Long ?: 0L
                    )
                }
                shelfItemCount = shelves.sumOf { it.magazineIds.size }

                // Count ratings
                val ratingDocs = db.collection("magazine_ratings")
                    .whereEqualTo("userId", uid)
                    .get().await()
                ratingCount = ratingDocs.size()

            } catch (e: Exception) { }
        }
    }

    LaunchedEffect(searchQuery) {
        delay(600)
        if (searchQuery != lastSearched) {
            lastSearched = searchQuery
            visibleRows = 3
            contentViewModel.fetchMagazines(searchQuery)
        }
    }

    val isSearching = searchQuery.isNotBlank()

    val archiveMagazines = when (val state = magazinesState) {
        is ContentState.Success -> state.items.map { it.toMagazineCover() }
        else -> emptyList()
    }

    val magazinesPerShelf = 4
    val allShelves = archiveMagazines.chunked(magazinesPerShelf)
    val visibleShelves = allShelves.take(visibleRows)

    fun openReader(magazine: MagazineCover) {
        if (!magazine.webUrl.isNullOrBlank()) {
            selectedMagazine = magazine
            readerVisible = true
            // ✅ Save reading progress
            currentUser?.uid?.let { uid ->
                val progressData = hashMapOf(
                    "magazineId" to magazine.id,
                    "magazineTitle" to magazine.title,
                    "magazineCoverUrl" to (magazine.coverImageUrl ?: ""),
                    "lastReadAt" to System.currentTimeMillis(),
                    "percentComplete" to 0.1f
                )
                FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .collection("reading_progress")
                    .document(magazine.id)
                    .set(progressData)
            }
        }
    }

    // Dialogs
    if (showCreateShelf && currentUser != null) {
        CreateShelfDialog(
            onDismiss = { showCreateShelf = false },
            onConfirm = { name, emoji ->
                val uid = currentUser!!.uid
                val shelfId = System.currentTimeMillis().toString()
                val shelfData = hashMapOf(
                    "name" to name,
                    "emoji" to emoji,
                    "magazineIds" to emptyList<String>(),
                    "createdAt" to System.currentTimeMillis()
                )
                FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .collection("magazine_shelves")
                    .document(shelfId)
                    .set(shelfData)
                    .addOnSuccessListener {
                        shelves = shelves + MagazineShelf(id = shelfId, name = name, emoji = emoji, createdAt = System.currentTimeMillis())
                    }
                showCreateShelf = false
            }
        )
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
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "MAGAZINES", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 32.sp, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        searchVisible = !searchVisible
                        if (!searchVisible) { searchQuery = ""; focusManager.clearFocus(); contentViewModel.fetchMagazines() }
                    }) {
                        Icon(imageVector = if (searchVisible) Icons.Filled.Close else Icons.Filled.Search, contentDescription = "Search", tint = ScrapbookDark, modifier = Modifier.size(24.dp))
                    }
                }
            }

            // ✅ Tab strip — BROWSE / SHELVES / CHALLENGES
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScrapbookCardWhite)
                    .border(BorderStroke(1.dp, ScrapbookBorder.copy(alpha = 0.2f)))
            ) {
                listOf("BROWSE", "SHELVES", "CHALLENGES").forEach { tab ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (activeTab == tab) ScrapbookYellow else ScrapbookCardWhite)
                            .clickable { activeTab = tab }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(tab, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp, letterSpacing = 0.5.sp)
                    }
                }
            }

            // Search bar
            AnimatedVisibility(visible = searchVisible, enter = expandVertically(), exit = shrinkVertically()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search retro magazines...", fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookTextMuted) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = ""; contentViewModel.fetchMagazines() }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear", tint = ScrapbookTextMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextDark),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ScrapbookDark,
                        unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f),
                        focusedContainerColor = ScrapbookCardWhite,
                        unfocusedContainerColor = ScrapbookCardWhite,
                        cursorColor = ScrapbookDark
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().background(ScrapbookCream).padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            when (activeTab) {
                "SHELVES" -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            MyShelvesSection(
                                shelves = shelves,
                                onCreateShelf = { showCreateShelf = true },
                                onShelfTap = { }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        if (currentUser == null) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("Sign in to create shelves!", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }

                "CHALLENGES" -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
                    ) {
                        item {
                            ReadingChallengesSection(
                                readCount = readHistory.size,
                                ratingCount = ratingCount,
                                shelfItemCount = shelfItemCount,
                                readEras = readHistory.mapNotNull { id ->
                                    sampleMagazineCovers.firstOrNull { it.id == id }?.era
                                        ?: archiveMagazines.firstOrNull { it.id == id }?.era
                                }
                            )
                        }
                    }
                }

                else -> { // BROWSE
                    when (val state = magazinesState) {
                        is ContentState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = ScrapbookYellowDark, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Loading magazines...", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
                                }
                            }
                        }
                        is ContentState.Error -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                                    Text(state.message, fontFamily = NunitoFontFamily, color = ScrapbookRed, fontSize = 13.sp, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookDark).border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)).clickable { contentViewModel.fetchMagazines() }.padding(horizontal = 24.dp, vertical = 10.dp)) {
                                        Text("RETRY", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                        is ContentState.Success -> {
                            if (isSearching) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(4),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(state.items, key = { it.id }) { item ->
                                        ScrapbookMagazineGridItem(
                                            item = item,
                                            isBookmarked = favoriteIds.contains(item.id),
                                            onBookmarkToggle = { favoritesViewModel?.toggleFavorite(item.toFavoriteItem()) },
                                            onClick = { openReader(item.toMagazineCover()) }
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 1.dp, bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(0.1.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // ✅ Continue Reading strip
                                    if (readingProgress.isNotEmpty()) {
                                        item {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            ContinueReadingStrip(
                                                progressList = readingProgress,
                                                onResume = { progress ->
                                                    val mag = archiveMagazines.firstOrNull { it.id == progress.magazineId }
                                                        ?: sampleMagazineCovers.firstOrNull { it.id == progress.magazineId }
                                                    mag?.let { openReader(it) }
                                                }
                                            )
                                        }
                                    }

                                    // Shelf rows
                                    itemsIndexed(visibleShelves) { _, shelfMagazines ->
                                        ShelfRow(
                                            magazinesOnShelf = shelfMagazines,
                                            shelfImageResId = R.drawable.shelf,
                                            favoriteIds = favoriteIds,
                                            onBookmarkToggle = { magazine ->
                                                val archiveItem = state.items.find { it.id == magazine.id }
                                                archiveItem?.let { favoritesViewModel?.toggleFavorite(it.toFavoriteItem()) }
                                            },
                                            onMagazineClick = { magazine -> openReader(magazine) },
                                            magazinesPerShelf = magazinesPerShelf
                                        )
                                    }

                                    // ✅ Because You Read section
                                    item {
                                        BecauseYouReadSection(
                                            readHistory = readHistory,
                                            allMagazines = sampleMagazineCovers + archiveMagazines,
                                            onMagazineTap = { openReader(it) }
                                        )
                                    }

                                    val totalRows = allShelves.size
                                    val canShowMore = visibleRows < totalRows || hasMore
                                    if (canShowMore) {
                                        item {
                                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                                if (isLoadingMore) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        CircularProgressIndicator(color = ScrapbookYellowDark, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                                        Text("Loading more...", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
                                                    }
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 32.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(ScrapbookYellow.copy(alpha = 0.2f))
                                                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                                                            .clickable {
                                                                val newVisible = visibleRows + 3
                                                                visibleRows = newVisible
                                                                if (newVisible >= totalRows && hasMore) contentViewModel.loadMoreMagazines()
                                                            }
                                                            .padding(vertical = 14.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("📚 VIEW MORE MAGAZINES", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, letterSpacing = 1.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else -> { }
                    }
                }
            }
        }

        // Reader overlay
        AnimatedVisibility(
            visible = readerVisible,
            enter = slideInVertically(tween(500, easing = LinearOutSlowInEasing)) { it } + fadeIn(tween(300)),
            exit = slideOutVertically(tween(400, easing = FastOutLinearInEasing)) { it } + fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            selectedMagazine?.let { magazine ->
                MagazineReaderScreen(
                    url = magazine.webUrl ?: "",
                    title = magazine.title,
                    magazineId = magazine.id,
                    onClose = {
                        readerVisible = false
                        selectedMagazine = null
                    }
                )
            }
        }

        // Ratings overlay
        if (showRatingsFor != null && currentUser != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { showRatingsFor = null }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.8f)
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(ScrapbookCream)
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ScrapbookYellow)
                                    .border(BorderStroke(2.dp, ScrapbookBorder))
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text("⭐ RATE & REVIEW", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { showRatingsFor = null }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = ScrapbookDark, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            MagazineRatingsSection(
                                magazineId = showRatingsFor!!.id,
                                currentUserId = currentUser!!.uid
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Unchanged composables ────────────────────────────────────────────────────

@Composable
fun ScrapbookMagazineGridItem(
    item: ArchiveItem,
    onClick: () -> Unit,
    isBookmarked: Boolean = false,
    onBookmarkToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f).clickable(onClick = onClick),
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 8.dp,
            shadowOffset = 2.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(model = item.thumbnailUrl, contentDescription = item.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().background(ScrapbookPaper))
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(3.dp).clip(CircleShape).background(ScrapbookYellow).border(1.dp, ScrapbookBorder, CircleShape)) {
                    IconButton(onClick = onBookmarkToggle, modifier = Modifier.size(22.dp)) {
                        Icon(imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(12.dp))
                    }
                }
                Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(ScrapbookDark.copy(alpha = 0.75f)).padding(4.dp)) {
                    Text(text = item.title, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 8.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun ArchiveMagazineGridItem(item: ArchiveItem, onClick: () -> Unit, isBookmarked: Boolean = false, onBookmarkToggle: () -> Unit = {}, modifier: Modifier = Modifier) {
    ScrapbookMagazineGridItem(item = item, onClick = onClick, isBookmarked = isBookmarked, onBookmarkToggle = onBookmarkToggle, modifier = modifier)
}

@Composable
fun MagazineCoverItem(
    magazine: MagazineCover,
    onClick: () -> Unit,
    isBookmarked: Boolean = false,
    onBookmarkToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f).clickable(onClick = onClick), backgroundColor = ScrapbookCardWhite, cornerRadius = 8.dp, shadowOffset = 2.dp) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (magazine.coverImageResId != null) {
                    Image(painter = painterResource(id = magazine.coverImageResId), contentDescription = magazine.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else if (magazine.coverImageUrl != null) {
                    AsyncImage(model = magazine.coverImageUrl, contentDescription = magazine.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().background(ScrapbookPaper))
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(ScrapbookPaper), contentAlignment = Alignment.Center) {
                        Text(text = magazine.title, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, textAlign = TextAlign.Center, fontSize = 9.sp, modifier = Modifier.padding(4.dp))
                    }
                }
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(3.dp).clip(CircleShape).background(ScrapbookYellow).border(1.dp, ScrapbookBorder, CircleShape)) {
                    IconButton(onClick = onBookmarkToggle, modifier = Modifier.size(22.dp)) {
                        Icon(imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(12.dp))
                    }
                }
                Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(ScrapbookDark.copy(alpha = 0.75f)).padding(4.dp)) {
                    Text(text = magazine.title, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 8.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun ShelfRow(
    magazinesOnShelf: List<MagazineCover>,
    shelfImageResId: Int,
    onMagazineClick: (MagazineCover) -> Unit,
    magazinesPerShelf: Int,
    favoriteIds: Set<String> = emptySet(),
    onBookmarkToggle: (MagazineCover) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.BottomCenter) {
        Image(painter = painterResource(id = shelfImageResId), contentDescription = "Magazine Shelf", modifier = Modifier.fillMaxWidth().matchParentSize(), contentScale = ContentScale.FillBounds)
        Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 140.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            magazinesOnShelf.forEach { magazine ->
                MagazineCoverItem(magazine = magazine, onClick = { onMagazineClick(magazine) }, isBookmarked = favoriteIds.contains(magazine.id), onBookmarkToggle = { onBookmarkToggle(magazine) }, modifier = Modifier.weight(1f))
            }
            if (magazinesOnShelf.size < magazinesPerShelf) {
                for (i in 0 until (magazinesPerShelf - magazinesOnShelf.size)) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
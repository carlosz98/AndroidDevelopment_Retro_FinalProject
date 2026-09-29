package com.example.hubretro

import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ─── Brand colors ─────────────────────────────────────────────────────────────
val TwitchPurple = CAcPurple
val YouTubeRed   = CAcRed
val TikTokPink   = Color(0xFFFF0050)
val TikTokCyan   = Color(0xFF00F2EA)

// ─── Palette helpers (cream theme) ────────────────────────────────────────────
private val CardBg      = Color.White.copy(alpha = 0.44f)          // comic glass
private val PageBg      = ComicGlassBg          // page background
private val BorderColor = ScrapbookDark
private val TextPrimary = ScrapbookDark
private val TextMuted   = ScrapbookTextMuted

// ─── Data ─────────────────────────────────────────────────────────────────────
data class TikTokCreator(
    val id: String,
    val username: String,
    val displayName: String,
    val niche: String,
    val followers: String,
    val emoji: String,
    val accentColor: Color,
    val profileUrl: String,
    val tags: List<String>
)

data class StreamHype(val fire: Int = 0, val heart: Int = 0, val controller: Int = 0)

val streamCategories = listOf("ALL", "RETRO", "RPG", "PLATFORMER", "FIGHTING", "RACING", "ARCADE")

val retrogamingTikTokCreators = listOf(
    TikTokCreator("t1", "@thenintendoking",    "The Nintendo King",   "Nintendo history & reviews",      "2.1M", "👑", Color(0xFFE4000F), "https://www.tiktok.com/@thenintendoking",    listOf("NES", "SNES")),
    TikTokCreator("t2", "@retrogaminghistory", "Retro Gaming History","Deep dives into gaming history",  "890K", "🕹️", CAcPurple, "https://www.tiktok.com/@retrogaminghistory", listOf("SEGA", "PS1")),
    TikTokCreator("t3", "@segafan_official",   "SEGA Fan Official",   "SEGA Genesis & Dreamcast",        "1.4M", "💿", CAcBlue, "https://www.tiktok.com/@segafan_official",   listOf("SEGA", "DC")),
    TikTokCreator("t4", "@pixelnostalgia",     "Pixel Nostalgia",     "Retro pixel art & reviews",       "670K", "🎨", CGreen, "https://www.tiktok.com/@pixelnostalgia",     listOf("PIXEL")),
    TikTokCreator("t5", "@arcadelegends",      "Arcade Legends",      "Classic arcade showcases",        "1.1M", "🕹️", CAcYellow, "https://www.tiktok.com/@arcadelegends",      listOf("ARCADE")),
    TikTokCreator("t6", "@ps1memories",        "PS1 Memories",        "PlayStation 1 nostalgia",         "780K", "💙", Color(0xFF003791), "https://www.tiktok.com/@ps1memories",        listOf("PS1", "PS2")),
    TikTokCreator("t7", "@gameboycollector",   "GameBoy Collector",   "Handheld gaming history",         "560K", "🎮", Color(0xFF8B4513), "https://www.tiktok.com/@gameboycollector",   listOf("GBA")),
    TikTokCreator("t8", "@retroboxart",        "Retro Box Art",       "Vintage game box art",            "430K", "🖼️", Color(0xFFFF1493), "https://www.tiktok.com/@retroboxart",        listOf("ART"))
)

// ─── Shimmer ──────────────────────────────────────────────────────────────────
@Composable
fun ShimmerStreamCard() {
    val shimmerT = rememberInfiniteTransition(label = "streamShimmer")
    val shimmerAlpha by shimmerT.animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse),
        label = "shimmerA"
    )
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(190.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(Color.White.copy(alpha = 0.92f).copy(alpha = shimmerAlpha))
        )
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                .background(CardBg)
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.fillMaxWidth(0.75f).height(16.dp).clip(RoundedCornerShape(4.dp))
                    .background(CGreen.copy(alpha = shimmerAlpha * 0.5f)))
                Box(modifier = Modifier.fillMaxWidth(0.45f).height(12.dp).clip(RoundedCornerShape(4.dp))
                    .background(BorderColor.copy(alpha = shimmerAlpha * 0.4f)))
            }
        }
    }
}

// ─── Pulsing LIVE badge ────────────────────────────────────────────────────────
@Composable
fun PulsingLiveBadge(small: Boolean = false) {
    val pulseT = rememberInfiniteTransition(label = "livePulse")
    val dotScale by pulseT.animateFloat(1f, 1.3f,
        infiniteRepeatable(tween(550, easing = EaseInOut), RepeatMode.Reverse), "liveDotScale")
    val dotAlpha by pulseT.animateFloat(0.5f, 1f,
        infiniteRepeatable(tween(550, easing = EaseInOut), RepeatMode.Reverse), "liveDotAlpha")
    Box(
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(1.5.dp, ScrapbookDark, RoundedCornerShape(8.dp))
            .padding(horizontal = if (small) 4.dp else 6.dp, vertical = if (small) 2.dp else 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(modifier = Modifier.scale(dotScale).size(if (small) 5.dp else 7.dp)
                .clip(CircleShape).background(CAcRed.copy(alpha = dotAlpha)))
            Text("LIVE", fontFamily = BangersFontFamily, color = ScrapbookDark,
                fontSize = if (small) 9.sp else 11.sp, letterSpacing = 0.5.sp)
        }
    }
}

// ─── Stream Hype Board ────────────────────────────────────────────────────────
@Composable
fun StreamHypeBoard(streamId: String) {
    var hype by remember { mutableStateOf(StreamHype()) }
    var myReactions by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(streamId) {
        try {
            val doc = FirebaseFirestore.getInstance().collection("stream_hype").document(streamId).get().await()
            if (doc.exists()) hype = StreamHype(
                fire       = (doc.getLong("fire") ?: 0).toInt(),
                heart      = (doc.getLong("heart") ?: 0).toInt(),
                controller = (doc.getLong("controller") ?: 0).toInt()
            )
        } catch (_: Exception) { }
    }

    fun react(type: String) {
        if (myReactions.contains(type)) return
        myReactions = myReactions + type
        val update = when (type) {
            "fire"       -> hype.copy(fire = hype.fire + 1)
            "heart"      -> hype.copy(heart = hype.heart + 1)
            "controller" -> hype.copy(controller = hype.controller + 1)
            else         -> hype
        }
        hype = update
        try { FirebaseFirestore.getInstance().collection("stream_hype").document(streamId)
            .set(mapOf("fire" to update.fire, "heart" to update.heart, "controller" to update.controller))
        } catch (_: Exception) { }
    }

    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
            .background(CardBg)
            .border(1.dp, BorderColor, RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("HYPE", fontFamily = BangersFontFamily, color = TextMuted, fontSize = 11.sp, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.width(12.dp))
            listOf(
                Triple("fire", "🔥", hype.fire),
                Triple("heart", "❤️", hype.heart),
                Triple("controller", "🎮", hype.controller)
            ).forEach { (type, emoji, count) ->
                val isReacted = myReactions.contains(type)
                var pressed by remember { mutableStateOf(false) }
                val btnScale by animateFloatAsState(
                    targetValue = if (pressed) 1.3f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                    label = "hypeBtn_$type"
                )
                Row(
                    modifier = Modifier.scale(btnScale)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isReacted) CGreen.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.04f))
                        .border(1.dp, if (isReacted) CGreen else BorderColor, RoundedCornerShape(8.dp))
                        .clickable { pressed = true; react(type) }
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(emoji, fontSize = 13.sp)
                    Text("$count", fontFamily = BangersFontFamily,
                        color = if (isReacted) ScrapbookDark else TextMuted, fontSize = 12.sp)
                }
                LaunchedEffect(pressed) { if (pressed) { delay(200); pressed = false } }
                Spacer(modifier = Modifier.width(6.dp))
            }
        }
    }
}

// ─── Embedded Twitch WebView ──────────────────────────────────────────────────
// Uses loadDataWithBaseURL so Twitch's parent-domain check passes in a native WebView.
@Composable
fun EmbeddedTwitchPlayer(channelName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    fun buildHtml(ch: String) = """
        <!DOCTYPE html>
        <html>
        <head><meta name="viewport" content="width=device-width,initial-scale=1"/></head>
        <body style="margin:0;padding:0;background:#000;overflow:hidden;">
          <iframe
            src="https://player.twitch.tv/?channel=$ch&parent=player.twitch.tv&autoplay=true&muted=false"
            style="width:100%;height:100vh;border:none;"
            allowfullscreen
            allow="autoplay;encrypted-media;picture-in-picture">
          </iframe>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled   = true
                settings.domStorageEnabled   = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.mixedContentMode    = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                settings.allowContentAccess  = true
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                webChromeClient = WebChromeClient()
                webViewClient   = WebViewClient()
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                loadDataWithBaseURL(
                    "https://player.twitch.tv",
                    buildHtml(channelName),
                    "text/html", "UTF-8", null
                )
            }
        },
        update = { wv ->
            wv.loadDataWithBaseURL(
                "https://player.twitch.tv",
                buildHtml(channelName),
                "text/html", "UTF-8", null
            )
        }
    )
}

// ─── Embedded TikTok WebView ──────────────────────────────────────────────────
@Composable
fun EmbeddedTikTokBrowser(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled   = true
                settings.domStorageEnabled   = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.mixedContentMode    = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                settings.allowContentAccess  = true
                settings.userAgentString     =
                    "Mozilla/5.0 (Linux; Android 13; Pixel 7) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/124.0.0.0 Mobile Safari/537.36"
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                webChromeClient = WebChromeClient()
                webViewClient   = WebViewClient()
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                loadUrl("https://www.tiktok.com/tag/retrogaming")
            }
        }
    )
}

// ─── YouTube Embed WebView ────────────────────────────────────────────────────
// loadDataWithBaseURL with https://www.youtube.com as base so the embed player
// thinks it is running on YouTube's own domain → no "video unavailable" errors.
private fun buildYTEmbedHtml(videoId: String) = """
    <!DOCTYPE html>
    <html>
    <head>
      <meta name="viewport" content="width=device-width,initial-scale=1,user-scalable=no">
      <style>
        * { margin:0; padding:0; box-sizing:border-box; }
        html,body { width:100%; height:100%; background:#000; overflow:hidden; }
        iframe { width:100%; height:100%; border:none; }
      </style>
    </head>
    <body>
      <iframe
        src="https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1&rel=0&modestbranding=1&controls=1&enablejsapi=1"
        allow="autoplay; encrypted-media; picture-in-picture; fullscreen"
        allowfullscreen>
      </iframe>
    </body>
    </html>
""".trimIndent()

@Composable
fun YouTubeEmbedPlayer(videoId: String, modifier: Modifier = Modifier) {
    val html = remember(videoId) { buildYTEmbedHtml(videoId) }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled  = true
                settings.domStorageEnabled  = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.mixedContentMode   = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                settings.allowContentAccess = true
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                webChromeClient = WebChromeClient()
                webViewClient   = WebViewClient()
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
            }
        },
        update = { wv ->
            wv.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
        }
    )
}

// ─── Streams Video Card (full-screen, glossy overlay) ────────────────────────
@Composable
fun StreamsVideoCard(short: RetroShort, isActive: Boolean, pageIndex: Int, totalPages: Int) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var isLiked   by remember { mutableStateOf(false) }
    var likeCount by remember { mutableStateOf(0) }
    var isLiking  by remember { mutableStateOf(false) }

    LaunchedEffect(isActive) {
        if (isActive) {
            isLiked   = fetchUserLiked(short.videoId)
            likeCount = fetchLikeCount(short.videoId)
        }
    }

    val glossBg = Brush.verticalGradient(
        listOf(Color.Transparent, Color(0x55000000), Color(0xCC000000), Color(0xEE000000))
    )
    val glassPanel = Brush.linearGradient(
        listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.07f))
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── Video area ──────────────────────────────────────────────────────
        if (isActive) {
            YouTubeEmbedPlayer(videoId = short.videoId, modifier = Modifier.fillMaxSize())
        } else {
            AsyncImage(
                model = short.thumbnailUrl,
                contentDescription = short.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.halftoneReveal(short.thumbnailUrl).fillMaxSize()
            )
            // Play hint while inactive
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.38f)),
                contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(68.dp).clip(CircleShape)
                    .background(glassPanel)
                    .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center) {
                    Text("▶", color = Color.White, fontSize = 28.sp)
                }
            }
        }

        // ── Bottom scrim ───────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize().background(glossBg))

        // ── Right-side action buttons ──────────────────────────────────────
        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Like
            var likePop by remember { mutableStateOf(false) }
            val likeScale by animateFloatAsState(
                targetValue = if (likePop) 1.4f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                label = "likeScale_$pageIndex"
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier.size(52.dp).scale(likeScale)
                        .clip(CircleShape)
                        .background(glassPanel)
                        .border(1.dp, if (isLiked) CGreen else Color.White.copy(alpha = 0.3f), CircleShape)
                        .clickable {
                            if (!isLiking) {
                                likePop = true; isLiking = true
                                scope.launch {
                                    val nowLiked = toggleRetroBytesLike(short.videoId)
                                    isLiked   = nowLiked
                                    likeCount = fetchLikeCount(short.videoId)
                                    isLiking  = false
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) { Text(if (isLiked) "❤️" else "🤍", fontSize = 22.sp) }
                Text(if (likeCount > 0) "$likeCount" else "Like",
                    fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
                    color = Color.White, fontSize = 11.sp)
            }
            LaunchedEffect(likePop) { if (likePop) { delay(200); likePop = false } }

            // Share
            var sharePressed by remember { mutableStateOf(false) }
            val shareScale by animateFloatAsState(
                targetValue = if (sharePressed) 0.88f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "shareScale_$pageIndex"
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier.size(52.dp).scale(shareScale)
                        .clip(CircleShape)
                        .background(glassPanel)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        .clickable {
                            sharePressed = true
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT,
                                    "Check this retro gaming short! 🎮\nhttps://youtube.com/shorts/${short.videoId}")
                            }
                            TvStaticBus.play { try { context.startActivity(Intent.createChooser(intent, "Share via")) } catch (_: Exception) { } }
                        },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Share, "Share", tint = Color.White, modifier = Modifier.size(22.dp)) }
                Text("Share", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
                    color = Color.White, fontSize = 11.sp)
            }
            LaunchedEffect(sharePressed) { if (sharePressed) { delay(150); sharePressed = false } }

            // Open in YouTube (glossy YT button)
            var ytPressed by remember { mutableStateOf(false) }
            val ytScale by animateFloatAsState(
                targetValue = if (ytPressed) 0.88f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "ytScale_$pageIndex"
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier.size(52.dp).scale(ytScale)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(YouTubeRed.copy(alpha = 0.5f), YouTubeRed.copy(alpha = 0.25f))))
                        .border(1.dp, YouTubeRed.copy(alpha = 0.7f), CircleShape)
                        .clickable {
                            ytPressed = true
                            try { TvStaticBus.play { try { context.startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://youtube.com/shorts/${short.videoId}"))) } catch (_: Exception) { } }
                            } catch (_: Exception) { }
                        },
                    contentAlignment = Alignment.Center
                ) { Text("▶", color = Color.White, fontSize = 20.sp) }
                Text("YouTube", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
                    color = Color.White, fontSize = 11.sp)
            }
            LaunchedEffect(ytPressed) { if (ytPressed) { delay(150); ytPressed = false } }
        }

        // ── Glossy bottom info panel ───────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Channel + title
            Column(modifier = Modifier
                .fillMaxWidth(0.78f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(34.dp).clip(CircleShape)
                        .background(glassPanel)
                        .border(1.5.dp, CGreen.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center) {
                        Text(short.channelName.take(1).uppercase(), fontFamily = BangersFontFamily,
                            color = CGreen, fontSize = 15.sp)
                    }
                    Text("@${short.channelName}", fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(short.title, fontFamily = BangersFontFamily, color = Color.White,
                    fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                    .background(glassPanel)
                    .border(1.dp, CGreen.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("🎮 RETRO GAMING", fontFamily = BangersFontFamily,
                        color = CGreen, fontSize = 10.sp, letterSpacing = 0.5.sp)
                }
            }

            // Glossy "Open in YouTube" bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(glassPanel)
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                    .clickable {
                        try { TvStaticBus.play { try { context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://youtube.com/shorts/${short.videoId}"))) } catch (_: Exception) { } }
                        } catch (_: Exception) { }
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Specular top line
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter)
                    .background(Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.5f), Color.Transparent))))
                Row(modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape)
                            .background(YouTubeRed.copy(alpha = 0.20f))
                            .border(1.dp, YouTubeRed.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center) {
                            Text("▶", color = YouTubeRed, fontSize = 14.sp)
                        }
                        Column {
                            Text("Open in YouTube", fontFamily = BangersFontFamily,
                                color = Color.White, fontSize = 14.sp)
                            Text("Watch full video or Shorts",
                                fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp)
                        }
                    }
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(Brush.linearGradient(listOf(CGreen, CGreenMint)))
                        .border(1.dp, ScrapbookDark.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("GO →", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp)
                    }
                }
            }
        }

        // ── Page indicator (right edge) ────────────────────────────────────
        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val start = (pageIndex - 2).coerceAtLeast(0)
            val end   = (pageIndex + 2).coerceAtMost(totalPages - 1)
            (start..end).forEach { i ->
                if (i == pageIndex) {
                    Box(modifier = Modifier.width(3.dp).height(20.dp).clip(RoundedCornerShape(2.dp))
                        .background(CGreen))
                } else {
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f)))
                }
            }
        }

        // ── RETROBYTES watermark ──────────────────────────────────────────
        Row(modifier = Modifier.align(Alignment.TopStart).padding(top = 12.dp, start = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("🎬", fontSize = 14.sp)
            Text("RETROBYTES", fontFamily = BangersFontFamily, color = CGreen,
                fontSize = 15.sp, letterSpacing = 2.sp)
        }
    }
}

// ─── Category Filter ──────────────────────────────────────────────────────────
@Composable
fun StreamCategoryFilter(selected: String, onSelect: (String) -> Unit) {
    val catEmoji = mapOf("ALL" to "🕹️", "RETRO" to "👾", "RPG" to "⚔️",
        "PLATFORMER" to "🎮", "FIGHTING" to "👊", "RACING" to "🏎️", "ARCADE" to "🪙")
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(streamCategories) { jumpIndex, cat ->
            Box(modifier = Modifier.jumpIn(jumpIndex)) {
            val isSelected = selected == cat
            var pressed by remember { mutableStateOf(false) }
            val chipScale by animateFloatAsState(
                targetValue = if (pressed) 0.92f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "catChip_$cat"
            )
            Box(
                modifier = Modifier.scale(chipScale)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) CGreen else CardBg)
                    .border(1.5.dp,
                        if (isSelected) ScrapbookDark else BorderColor,
                        RoundedCornerShape(20.dp))
                    .shadow(if (isSelected) 2.dp else 0.dp, RoundedCornerShape(20.dp))
                    .clickable { pressed = true; onSelect(cat) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text("${catEmoji[cat] ?: "🎮"} $cat", fontFamily = BangersFontFamily,
                    color = if (isSelected) ScrapbookDark else TextMuted, fontSize = 12.sp)
            }
            LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
                    }
        }
    }
}

// ─── Game filter bar ──────────────────────────────────────────────────────────
@Composable
fun GameFilterBar(query: String, onQueryChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = query, onValueChange = onQueryChange,
        placeholder = { Text("Filter by game…", fontFamily = NunitoFontFamily, fontSize = 13.sp, color = TextMuted) },
        leadingIcon = { Icon(Icons.Filled.Search, null, tint = CGreen, modifier = Modifier.size(18.dp)) },
        trailingIcon = {
            if (query.isNotEmpty()) IconButton({ onQueryChange("") }) {
                Icon(Icons.Filled.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, color = TextPrimary),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor     = ScrapbookDark,
            unfocusedBorderColor   = BorderColor,
            focusedContainerColor  = CardBg,
            unfocusedContainerColor= CardBg,
            cursorColor            = ScrapbookDark,
            focusedTextColor       = TextPrimary,
            unfocusedTextColor     = TextPrimary
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    )
}

// ─── Twitch Stream Card ───────────────────────────────────────────────────────
@Composable
fun TwitchStreamCard(stream: TwitchStream, isActive: Boolean = false, onClick: () -> Unit) {
    val glowT = rememberInfiniteTransition(label = "twitchGlow")
    val glowAlpha by rememberGlowPhase(0.4f)
    val borderAlpha by glowT.animateFloat(
        if (isActive) 0.5f else 0.2f, if (isActive) 1f else 0.35f,
        infiniteRepeatable(tween(1400, easing = EaseInOut), RepeatMode.Reverse),
        "twitchBorderAlpha"
    )
    var pressed by remember { mutableStateOf(false) }
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "twitchPress")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "twitchShadow")

    Box(modifier = Modifier.fillMaxWidth()) {
        // Outer glow
        Box(modifier = Modifier.matchParentSize().offset(6.dp, 6.dp).clip(RoundedCornerShape(14.dp)).background(CGreen.copy(alpha = glowAlpha * 0.25f)))
        // Hard shadow
        Box(modifier = Modifier.matchParentSize().offset(shadowOff.dp, shadowOff.dp).clip(RoundedCornerShape(14.dp)).background(CGreen))
    Column(
        modifier = Modifier.fillMaxWidth().offset(y = pressAnim.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(2.dp,
                if (isActive) TwitchPurple.copy(alpha = borderAlpha)
                else BorderColor,
                RoundedCornerShape(14.dp))
    ) {
        // Video area (always dark / black)
        Box(
            modifier = Modifier.fillMaxWidth().height(190.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(Color.Black)
                .retroTvBezel()
        ) {
            if (isActive) {
                EmbeddedTwitchPlayer(
                    channelName = stream.userName,
                    modifier    = Modifier.fillMaxSize()
                )
            } else {
                // Higher-res thumbnail
                AsyncImage(
                    model = stream.thumbnailUrl.replace("320x180", "1280x720"),
                    contentDescription = stream.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.halftoneReveal(stream.thumbnailUrl.replace("320x180", "1280x720")).fillMaxSize()
                )
                // Dim overlay
                Box(modifier = Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.30f)))
                // Play button
                Box(
                    modifier = Modifier.size(52.dp).align(Alignment.Center)
                        .clip(CircleShape).background(Color.Black.copy(alpha = 0.55f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text("▶", color = Color.White, fontSize = 20.sp) }
            }

            // LIVE badge
            Box(modifier = Modifier.align(Alignment.TopStart).padding(9.dp)) { PulsingLiveBadge() }

            // Viewer count
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(9.dp)
                    .clip(RoundedCornerShape(5.dp)).background(Color.Black.copy(alpha = 0.70f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Person, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(10.dp))
                    Text("${formatViewerCount(stream.viewerCount)}", fontFamily = BangersFontFamily,
                        color = Color.White, fontSize = 12.sp)
                }
            }

            // NOW PLAYING pill
            if (isActive) {
                Box(
                    modifier = Modifier.align(Alignment.BottomStart).padding(9.dp)
                        .clip(RoundedCornerShape(5.dp)).background(TwitchPurple)
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color.White))
                        Text("NOW PLAYING", fontFamily = BangersFontFamily, color = Color.White, fontSize = 10.sp)
                    }
                }
            }
        }

        // Info row (cream)
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(CardBg)
                .clickable { pressed = true; onClick() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(TwitchPurple.copy(alpha = 0.15f))
                    .border(1.5.dp, TwitchPurple.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(stream.userName.first().uppercase(), fontFamily = BangersFontFamily,
                    color = TwitchPurple, fontSize = 14.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(stream.title, fontFamily = BangersFontFamily, color = TextPrimary,
                    fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stream.userName, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
                    color = TwitchPurple, fontSize = 12.sp)
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .background(CGreen)
                    .border(1.dp, ScrapbookDark.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(stream.gameName, fontFamily = BangersFontFamily, color = ScrapbookDark,
                    fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        StreamHypeBoard(streamId = stream.id)
    } // end Column
    } // end outer glow Box
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Featured Live Hero ───────────────────────────────────────────────────────
@Composable
fun FeaturedStreamHero(stream: TwitchStream, isPlaying: Boolean, onClick: () -> Unit) {
    val pulseT = rememberInfiniteTransition(label = "heroNeon")
    val borderAlpha by rememberGlowRange(0.5f, 1f)
    val kenBurns by pulseT.animateFloat(1.0f, 1.08f,
        infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse), "heroKB")

    var pressed by remember { mutableStateOf(false) }
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "heroPress")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "heroShadow")

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Box(modifier = Modifier.matchParentSize().offset(6.dp, 6.dp).clip(RoundedCornerShape(18.dp)).background(TwitchPurple.copy(alpha = borderAlpha * 0.25f)))
        Box(modifier = Modifier.matchParentSize().offset(shadowOff.dp, shadowOff.dp).clip(RoundedCornerShape(18.dp)).background(CGreen))
    Column(
        modifier = Modifier.fillMaxWidth().offset(y = pressAnim.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .border(2.dp, TwitchPurple.copy(alpha = borderAlpha), RoundedCornerShape(18.dp))
            .clickable { pressed = true; onClick() }
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(230.dp)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(Color.Black)
                .retroTvBezel()
        ) {
            if (isPlaying) {
                EmbeddedTwitchPlayer(channelName = stream.userName, modifier = Modifier.fillMaxSize())
            } else {
                AsyncImage(
                    model = stream.thumbnailUrl.replace("320x180", "1280x720"),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.halftoneReveal(stream.thumbnailUrl.replace("320x180", "1280x720")).fillMaxSize().scale(kenBurns)
                )
                Box(modifier = Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))))
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                    Text(stream.title, fontFamily = BangersFontFamily, color = Color.White,
                        fontSize = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 24.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stream.userName, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
                            color = TwitchPurple, fontSize = 13.sp)
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(CGreen)
                            .padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text(stream.gameName, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp)
                        }
                    }
                }
            }
            // Badges
            Row(modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PulsingLiveBadge()
                Box(modifier = Modifier.clip(RoundedCornerShape(5.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, CGreen.copy(alpha = 0.5f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 9.dp, vertical = 3.dp)) {
                    Text("👑 FEATURED", fontFamily = BangersFontFamily, color = CGreen, fontSize = 11.sp)
                }
            }
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                .clip(RoundedCornerShape(5.dp)).background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 9.dp, vertical = 3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Red))
                    Text("${formatViewerCount(stream.viewerCount)}", fontFamily = BangersFontFamily, color = Color.White, fontSize = 13.sp)
                }
            }
        }
        StreamHypeBoard(streamId = stream.id)
    } // end Column
    } // end outer glow Box
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── YouTube Video Card ───────────────────────────────────────────────────────
@Composable
fun YouTubeVideoCard(video: YouTubeVideo, onClick: () -> Unit) {
    val glowT = rememberInfiniteTransition(label = "ytGlow")
    val glowAlpha by rememberGlowPhase(0.4f)
    val playScale by glowT.animateFloat(1f, 1.12f,
        infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse), "ytPlayScale")
    var pressed by remember { mutableStateOf(false) }
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "ytPress")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "ytShadow")

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.matchParentSize().offset(6.dp, 6.dp).clip(RoundedCornerShape(14.dp)).background(CGreen.copy(alpha = glowAlpha * 0.25f)))
        Box(modifier = Modifier.matchParentSize().offset(shadowOff.dp, shadowOff.dp).clip(RoundedCornerShape(14.dp)).background(CGreen))
    Column(
        modifier = Modifier.fillMaxWidth().offset(y = pressAnim.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.5.dp, BorderColor, RoundedCornerShape(14.dp))
            .clickable { pressed = true; onClick() }
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            .background(Color.Black).retroTvBezel()) {
            AsyncImage(model = video.thumbnailUrl, contentDescription = video.title,
                contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(video.thumbnailUrl).fillMaxSize())
            Box(modifier = Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)))))
            Box(modifier = Modifier.size(52.dp).align(Alignment.Center).scale(playScale)
                .clip(CircleShape).background(Color.Black.copy(alpha = 0.55f))
                .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center) { Text("▶", color = Color.White, fontSize = 20.sp) }
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                .clip(RoundedCornerShape(4.dp)).background(YouTubeRed)
                .padding(horizontal = 7.dp, vertical = 2.dp)) {
                Text("YT", fontFamily = BangersFontFamily, color = Color.White, fontSize = 10.sp)
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                .background(YouTubeRed.copy(alpha = 0.12f))
                .border(1.5.dp, YouTubeRed.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center) {
                Text("▶", color = YouTubeRed, fontSize = 14.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(video.title, fontFamily = BangersFontFamily, color = TextPrimary,
                    fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                Text(video.channelTitle, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
                    color = YouTubeRed, fontSize = 11.sp)
            }
        }
    } // end Column
    } // end outer glow Box
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Clip of the Day ─────────────────────────────────────────────────────────
@Composable
fun ClipOfTheDayCard(video: YouTubeVideo, onClick: () -> Unit) {
    val pulseT = rememberInfiniteTransition(label = "clipNeon")
    val crownScale by pulseT.animateFloat(1f, 1.18f,
        infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse), "clipCrown")
    val kenBurns by pulseT.animateFloat(1f, 1.07f,
        infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse), "clipKB")

    var pressed by remember { mutableStateOf(false) }
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "clipPress")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "clipShadow")

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            Box(modifier = Modifier.matchParentSize().offset(6.dp, 6.dp).clip(RoundedCornerShape(18.dp)).background(CGreen.copy(alpha = crownScale * 0.08f))) // glow tied to crown pulse
        Box(modifier = Modifier.matchParentSize().offset(shadowOff.dp, shadowOff.dp).clip(RoundedCornerShape(18.dp)).background(CGreen))
    Column(
        modifier = Modifier.fillMaxWidth().offset(y = pressAnim.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .border(2.dp, YouTubeRed.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .clickable { pressed = true; onClick() }
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(210.dp)
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)).background(Color.Black)) {
            AsyncImage(model = video.thumbnailUrl, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(video.thumbnailUrl).fillMaxSize().scale(kenBurns))
            Box(modifier = Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))
            Row(modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(modifier = Modifier.scale(crownScale)) { Text("👑", fontSize = 20.sp) }
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(YouTubeRed)
                    .padding(horizontal = 9.dp, vertical = 3.dp)) {
                    Text("CLIP OF THE DAY", fontFamily = BangersFontFamily, color = Color.White, fontSize = 10.sp)
                }
            }
            Box(modifier = Modifier.size(60.dp).align(Alignment.Center).clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f)).border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center) { Text("▶", color = Color.White, fontSize = 24.sp) }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(video.title, fontFamily = BangersFontFamily, color = TextPrimary,
                    fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 22.sp)
                Spacer(modifier = Modifier.height(3.dp))
                Text(video.channelTitle, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
                    color = YouTubeRed, fontSize = 12.sp)
            }
            RetroGlassButton(text = "WATCH", onClick = { pressed = true; onClick() }, cornerRadius = 10.dp)
        }
    } // end Column
    } // end outer glow Box
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Main Screen ──────────────────────────────────────────────────────────────
@Composable
fun StreamsScreen(
    modifier: Modifier = Modifier,
    streamsViewModel: StreamsViewModel = viewModel()
) {
    val twitchState  by streamsViewModel.twitchStreams.collectAsState()
    val youtubeState by streamsViewModel.youtubeVideos.collectAsState()
    var selectedTab  by remember { mutableStateOf(0) }
    val tabs = listOf("🔴 LIVE", "🎬 VIDEOS", "📱 TIKTOK")

    val liveCount = when (val s = twitchState) {
        is StreamsState.Success<*> -> (s.data as? List<TwitchStream>)?.size ?: 0
        else -> 0
    }

    Box(modifier = modifier.fillMaxSize().background(PageBg)) {
        HalftoneBackground(modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize()) {

            // ─── Header ──────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()
                .background(CardBg)
                .border(BorderStroke(1.dp, BorderColor))
            ) {
                // Tape-strip top accent
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).align(Alignment.TopCenter)
                    .background(CGreen))
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp).padding(top = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📺", fontSize = 20.sp)
                                Text("STREAMS", fontFamily = BangersFontFamily, color = TextPrimary,
                                    fontSize = 26.sp, letterSpacing = 2.sp)
                            }
                            Text("live • clips • tiktok", fontFamily = NunitoFontFamily,
                                color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (liveCount > 0) {
                                Box(modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                    .background(Color.Red.copy(alpha = 0.10f))
                                    .border(1.dp, Color.Red.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    val pulseT = rememberInfiniteTransition(label = "liveDot")
                                    val dotA by pulseT.animateFloat(0.4f, 1f,
                                        infiniteRepeatable(tween(600, easing = EaseInOut), RepeatMode.Reverse), "headerDot")
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color.Red.copy(alpha = dotA)))
                                        Text("$liveCount LIVE", fontFamily = BangersFontFamily, color = Color.Red, fontSize = 12.sp)
                                    }
                                }
                            }
                            Box(modifier = Modifier.size(38.dp).clip(CircleShape)
                                .background(CGreen.copy(alpha = 0.20f))
                                .border(1.dp, ScrapbookDark.copy(alpha = 0.30f), CircleShape)
                                .clickable { streamsViewModel.fetchAll() },
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Refresh, "Refresh", tint = ScrapbookDark, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // ─── Tab strip ───────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth().background(CardBg)
                .border(BorderStroke(1.dp, BorderColor))
                .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    var tabPressed by remember { mutableStateOf(false) }
                    val tabScale by animateFloatAsState(
                        targetValue = if (tabPressed) 0.93f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "tab_$index"
                    )
                    Box(
                        modifier = Modifier.weight(1f).scale(tabScale)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) CGreen else PageBg)
                            .border(1.dp,
                                if (isSelected) ScrapbookDark.copy(alpha = 0.5f) else BorderColor,
                                RoundedCornerShape(10.dp))
                            .clickable { tabPressed = true; selectedTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(title, fontFamily = BangersFontFamily,
                            color = if (isSelected) ScrapbookDark else TextMuted,
                            fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                    LaunchedEffect(tabPressed) { if (tabPressed) { delay(150); tabPressed = false } }
                }
            }

            // ─── Content ─────────────────────────────────────────────────────
            when (selectedTab) {
                0 -> TwitchStreamsTab(state = twitchState, onRetry = { streamsViewModel.fetchAll() })
                1 -> YouTubeVideosTab(state = youtubeState, onRetry = { streamsViewModel.fetchAll() })
                2 -> TikTokTab()
            }
        }
    }
}

// ─── Twitch Live Tab ─────────────────────────────────────────────────────────
@Composable
fun TwitchStreamsTab(state: StreamsState, onRetry: () -> Unit = {}) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("ALL") }
    var gameFilter       by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    when (state) {
        is StreamsState.Loading -> LazyColumn(modifier = Modifier.fillMaxSize().background(PageBg),
            contentPadding = PaddingValues(vertical = 12.dp)) { item { NowLoadingIndicator(label = "TUNING IN") }; items(3) { ShimmerStreamCard() } }
        is StreamsState.Error   -> StreamsErrorState(message = state.message, onRetry = onRetry)
        is StreamsState.Empty   -> StreamsEmptyState(emoji = "📺", title = "NO LIVE STREAMS",
            subtitle = "No retro gaming streams live right now.\nCheck back later!")
        is StreamsState.Success<*> -> {
            @Suppress("UNCHECKED_CAST")
            val streams = state.data as List<TwitchStream>
            val featuredStream = streams.maxByOrNull { it.viewerCount }
            val filteredStreams = remember(streams, selectedCategory, gameFilter) {
                streams.filter { s ->
                    (selectedCategory == "ALL" || s.gameName.contains(selectedCategory, ignoreCase = true)) &&
                    (gameFilter.isBlank() || s.gameName.contains(gameFilter, ignoreCase = true) || s.title.contains(gameFilter, ignoreCase = true))
                }
            }

            // Header offset: featured hero + filter section = 2 items
            val headerCount = if (featuredStream != null) 2 else 1
            val activeStreamIndex by remember {
                derivedStateOf {
                    val info   = listState.layoutInfo
                    val vpMid  = (info.viewportStartOffset + info.viewportEndOffset) / 2
                    val closest = info.visibleItemsInfo
                        .filter { it.index >= headerCount }
                        .minByOrNull { kotlin.math.abs((it.offset + it.size / 2) - vpMid) }
                    closest?.index?.minus(headerCount) ?: -1
                }
            }

            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().background(PageBg),
                contentPadding = PaddingValues(bottom = 80.dp)) {

                featuredStream?.let { hero ->
                    item(key = "featured") {
                        Spacer(modifier = Modifier.height(8.dp))
                        FeaturedStreamHero(
                            stream    = hero,
                            isPlaying = activeStreamIndex == -1,
                            onClick   = { TvStaticBus.play { try { context.startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://www.twitch.tv/${hero.userName}"))) } catch (_: Exception) { } } }
                        )
                    }
                }

                item(key = "filters") {
                    Spacer(modifier = Modifier.height(14.dp))
                    // Section label
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp))
                            .background(Color.Red))
                        Spacer(modifier = Modifier.width(9.dp))
                        Text("ALL STREAMS", fontFamily = BangersFontFamily, color = TextPrimary,
                            fontSize = 20.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.clip(RoundedCornerShape(7.dp))
                            .background(CGreen.copy(alpha = 0.7f))
                            .border(1.dp, BorderColor, RoundedCornerShape(7.dp))
                            .padding(horizontal = 9.dp, vertical = 3.dp)) {
                            Text("${filteredStreams.size}", fontFamily = BangersFontFamily,
                                color = ScrapbookDark, fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Scroll-to-play hint
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBg)
                        .border(1.dp, TwitchPurple.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("▶", color = TwitchPurple, fontSize = 11.sp)
                            Text("Scroll a stream to center — it plays automatically",
                                fontFamily = NunitoFontFamily, color = TextMuted, fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    StreamCategoryFilter(selected = selectedCategory, onSelect = { selectedCategory = it })
                    Spacer(modifier = Modifier.height(8.dp))
                    GameFilterBar(query = gameFilter, onQueryChange = { gameFilter = it })
                    Spacer(modifier = Modifier.height(8.dp))
                }

                itemsIndexed(filteredStreams, key = { _, s -> s.id }) { index, stream ->
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                        TwitchStreamCard(
                            stream   = stream,
                            isActive = index == activeStreamIndex,
                            onClick  = { TvStaticBus.play { try { context.startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://www.twitch.tv/${stream.userName}"))) } catch (_: Exception) { } } }
                        )
                    }
                }

                if (filteredStreams.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🔍", fontSize = 44.sp)
                                Text("NO STREAMS MATCH", fontFamily = BangersFontFamily,
                                    color = TextPrimary, fontSize = 20.sp)
                                Text("Try a different category or filter",
                                    fontFamily = NunitoFontFamily, color = TextMuted, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── YouTube Videos Tab (VerticalPager auto-play, WebView embed, no black flicker) ──
@Composable
fun YouTubeVideosTab(state: StreamsState, onRetry: () -> Unit = {}) {
    val context = LocalContext.current
    var shorts        by remember { mutableStateOf<List<RetroShort>>(emptyList()) }
    var shortsLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        shortsLoading = true
        shorts        = fetchRetroBytesShorts()
        shortsLoading = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when {
            shortsLoading -> {
                val shT = rememberInfiniteTransition(label = "vidShimmer")
                val shA by shT.animateFloat(0.3f, 1f,
                    infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse), "shA")
                Column(modifier = Modifier.fillMaxSize().background(PageBg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Text("🎬", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("LOADING VIDEOS", fontFamily = BangersFontFamily,
                        color = ScrapbookDark.copy(alpha = shA), fontSize = 20.sp, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Fetching retro gaming clips…", fontFamily = NunitoFontFamily,
                        color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(24.dp))
                    CircularProgressIndicator(color = CGreen, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                }
            }
            shorts.isNotEmpty() -> {
                val pagerState = rememberPagerState(pageCount = { shorts.size })

                // Only mark a page "active" once scrolling has fully settled —
                // prevents the black-screen flash that occurs mid-swipe when the
                // previous WebView tears down before the next one renders.
                val activePage by remember {
                    derivedStateOf {
                        if (!pagerState.isScrollInProgress) pagerState.currentPage else -1
                    }
                }

                VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    StreamsVideoCard(
                        short      = shorts[page],
                        isActive   = activePage == page,
                        pageIndex  = page,
                        totalPages = shorts.size
                    )
                }
            }
            else -> when (state) {
                is StreamsState.Loading -> LazyColumn(modifier = Modifier.fillMaxSize().background(PageBg),
                    contentPadding = PaddingValues(vertical = 12.dp)) { item { NowLoadingIndicator(label = "TUNING IN") }; items(3) { ShimmerStreamCard() } }
                is StreamsState.Error   -> StreamsErrorState(message = state.message, onRetry = onRetry)
                is StreamsState.Empty   -> StreamsEmptyState(emoji = "🎬", title = "NO VIDEOS",
                    subtitle = "No retro gaming videos found.\nCheck back later!")
                is StreamsState.Success<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val videos = state.data as List<YouTubeVideo>
                    LazyColumn(modifier = Modifier.fillMaxSize().background(PageBg),
                        contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)) {
                        itemsIndexed(videos, key = { _, v -> v.id }) { _, video ->
                            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                                YouTubeVideoCard(video = video, onClick = {
                                    TvStaticBus.play { try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(video.videoUrl))) } catch (_: Exception) { } }
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── TikTok Tab ───────────────────────────────────────────────────────────────
@Composable
fun TikTokTab() {
    val context = LocalContext.current
    var showBrowser by remember { mutableStateOf(false) }

    if (showBrowser) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            EmbeddedTikTokBrowser(modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.70f))
                .border(1.dp, TikTokPink.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .clickable { showBrowser = false }
                .padding(horizontal = 14.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Text("CLOSE", fontFamily = BangersFontFamily, color = Color.White, fontSize = 11.sp)
                }
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().background(PageBg),
            contentPadding = PaddingValues(bottom = 80.dp)) {

            // Hero launch card
            item {
                Spacer(modifier = Modifier.height(16.dp))
                val borderAlpha by rememberGlowRange(0.4f, 1f)
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        .shadow(4.dp, RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        .background(CardBg)
                        .border(2.5.dp, ScrapbookDark, RoundedCornerShape(18.dp))
                        .clickable { showBrowser = true }
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.size(70.dp).clip(RoundedCornerShape(16.dp))
                        .background(Brush.radialGradient(listOf(TikTokPink.copy(alpha = 0.15f), CardBg)))
                        .border(1.5.dp, TikTokPink.copy(alpha = borderAlpha * 0.6f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center) { Text("📱", fontSize = 36.sp) }
                    Text("TIKTOK RETRO GAMING", fontFamily = BangersFontFamily, color = TextPrimary,
                        fontSize = 20.sp, letterSpacing = 1.sp, textAlign = TextAlign.Center)
                    Text("Watch real retro gaming TikToks — tap to launch the live feed",
                        fontFamily = NunitoFontFamily, color = TextMuted, fontSize = 12.sp,
                        textAlign = TextAlign.Center, lineHeight = 18.sp)
                    Box(modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(listOf(TikTokPink, CAcRed, TikTokCyan.copy(alpha = 0.8f))))
                        .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("▶", color = Color.White, fontSize = 16.sp)
                            Text("WATCH TIKTOK VIDEOS", fontFamily = BangersFontFamily, color = Color.White, fontSize = 15.sp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .background(TikTokPink.copy(alpha = 0.12f))
                            .border(1.dp, TikTokPink.copy(alpha = 0.40f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("#RETROGAMING", fontFamily = BangersFontFamily, color = TikTokPink, fontSize = 10.sp)
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .background(TikTokCyan.copy(alpha = 0.12f))
                            .border(1.dp, TikTokCyan.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("LIVE FEED", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 10.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Open in TikTok App
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    .shadow(1.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBg)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .clickable {
                        try { TvStaticBus.play { try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com/tag/retrogaming"))) } catch (_: Exception) { } } }
                        catch (_: Exception) { }
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(TikTokPink.copy(alpha = 0.12f))
                            .border(1.dp, TikTokPink.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.OpenInNew, null, tint = TikTokPink, modifier = Modifier.size(18.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Open in TikTok App", fontFamily = BangersFontFamily, color = TextPrimary, fontSize = 14.sp)
                            Text("Browse #retrogaming natively", fontFamily = NunitoFontFamily,
                                color = TextMuted, fontSize = 11.sp)
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Section header
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp))
                        .background(Brush.verticalGradient(listOf(TikTokPink, TikTokCyan))))
                    Spacer(modifier = Modifier.width(9.dp))
                    Text("TOP CREATORS", fontFamily = BangersFontFamily, color = TextPrimary,
                        fontSize = 18.sp, letterSpacing = 0.5.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Tap to visit their profile", fontFamily = NunitoFontFamily,
                    color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Creator grid
            val rows = retrogamingTikTokCreators.chunked(2)
            itemsIndexed(rows) { _, rowCreators ->
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowCreators.forEach { creator ->
                        Box(modifier = Modifier.weight(1f)) {
                            ImprovedTikTokCreatorCard(creator = creator) {
                                try { TvStaticBus.play { try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(creator.profileUrl))) } catch (_: Exception) { } } }
                                catch (_: Exception) { }
                            }
                        }
                    }
                    if (rowCreators.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// ─── TikTok Creator Card (cream) ─────────────────────────────────────────────
@Composable
fun ImprovedTikTokCreatorCard(creator: TikTokCreator, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "tikCard"
    )
    Column(
        modifier = Modifier.scale(cardScale)
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.5.dp, creator.accentColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable { pressed = true; onClick() }
    ) {
        // Top accent bar
        Box(modifier = Modifier.fillMaxWidth().height(3.dp)
            .background(Brush.horizontalGradient(listOf(creator.accentColor, creator.accentColor.copy(alpha = 0.4f)))))
        Column(modifier = Modifier.padding(12.dp).padding(top = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(56.dp).clip(CircleShape)
                .background(creator.accentColor.copy(alpha = 0.12f))
                .border(2.dp, creator.accentColor.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center) { Text(creator.emoji, fontSize = 26.sp) }
            Spacer(modifier = Modifier.height(8.dp))
            Text(creator.username, fontFamily = BangersFontFamily, color = creator.accentColor,
                fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Text(creator.displayName, fontFamily = NunitoFontFamily, color = TextMuted,
                fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(5.dp))
            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                .background(creator.accentColor.copy(alpha = 0.12f))
                .border(1.dp, creator.accentColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(horizontal = 9.dp, vertical = 3.dp)) {
                Text(creator.followers, fontFamily = BangersFontFamily, color = creator.accentColor, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(creator.niche, fontFamily = NunitoFontFamily, color = TextMuted,
                fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                creator.tags.take(2).forEach { tag ->
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                        .background(CGreen.copy(alpha = 0.35f))
                        .border(1.dp, ScrapbookDark, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(tag, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 9.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .background(Brush.horizontalGradient(listOf(TikTokPink, TikTokCyan.copy(alpha = 0.8f))))
                .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center) {
                Text("VIEW PROFILE", fontFamily = BangersFontFamily, color = Color.White, fontSize = 11.sp)
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Empty & Error States (cream) ─────────────────────────────────────────────
@Composable
fun StreamsEmptyState(emoji: String, title: String, subtitle: String) {
    Box(modifier = Modifier.fillMaxSize().background(PageBg), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
                .shadow(2.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(CardBg)
                .border(1.dp, BorderColor, RoundedCornerShape(18.dp))
                .padding(28.dp)
        ) {
            Text(emoji, fontSize = 52.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontFamily = BangersFontFamily, color = TextPrimary, fontSize = 22.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text(subtitle, fontFamily = NunitoFontFamily, color = TextMuted, fontSize = 13.sp,
                textAlign = TextAlign.Center, lineHeight = 20.sp)
        }
    }
}

@Composable
fun StreamsErrorState(message: String, onRetry: (() -> Unit)? = null) {
    Box(modifier = Modifier.fillMaxSize().background(PageBg), contentAlignment = Alignment.Center) {
        GameOverState(message = message, onRetry = onRetry)
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────
fun formatViewerCount(count: Int): String = when {
    count >= 1_000_000 -> "${count / 1_000_000}M"
    count >= 1_000     -> "${count / 1_000}K"
    else               -> count.toString()
}

@Composable fun EmptyState(emoji: String, title: String, subtitle: String) = StreamsEmptyState(emoji, title, subtitle)
@Composable fun ErrorState(message: String) = StreamsErrorState(message)


// ─── Retro CRT TV bezel around stream / video thumbnails ──────────────────────

fun Modifier.retroTvBezel(): Modifier = this
    .background(ScrapbookDark)
    .drawBehind {
        val y = size.height - 8.dp.toPx()
        // power LED + speaker slits (left), two knobs (right)
        drawCircle(CGreen, radius = 2.5.dp.toPx(), center = Offset(14.dp.toPx(), y))
        for (i in 0..3) {
            val x = 26.dp.toPx() + i * 5.dp.toPx()
            drawLine(Color(0xFF4A4A4A), Offset(x, y - 3.dp.toPx()), Offset(x, y + 3.dp.toPx()), strokeWidth = 1.5.dp.toPx())
        }
        drawCircle(Color(0xFF5A5A5A), radius = 4.dp.toPx(), center = Offset(size.width - 16.dp.toPx(), y))
        drawCircle(Color(0xFF5A5A5A), radius = 4.dp.toPx(), center = Offset(size.width - 30.dp.toPx(), y))
    }
    .padding(start = 7.dp, end = 7.dp, top = 7.dp, bottom = 16.dp)
    .clip(RoundedCornerShape(12.dp))
    .drawWithContent {
        drawContent()
        // scanlines + CRT vignette over the picture
        val step = 3.dp.toPx()
        var yy = 0f
        while (yy < size.height) {
            drawLine(Color.Black.copy(alpha = 0.10f), Offset(0f, yy), Offset(size.width, yy), strokeWidth = 1f)
            yy += step
        }
        drawRect(Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
            center = center, radius = size.maxDimension * 0.75f
        ))
    }

package com.example.hubretro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.hubretro.ui.theme.*
import androidx.compose.ui.draw.blur

// ─── Talking Robot ────────────────────────────────────────────────────────────

@Composable
fun TalkingRobot(
    message: String,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    robotSpriteResId: Int? = null,
    habboUsername: String = "",
    habboRegion: String = "habbo.com",       // ✅ region param
    showHabboAvatar: Boolean = true
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            initialOffsetX = { it / 2 },
            animationSpec = tween(500)
        ) + fadeIn(animationSpec = tween(500)),
        exit = slideOutHorizontally(
            targetOffsetX = { it / 2 },
            animationSpec = tween(500)
        ) + fadeOut(animationSpec = tween(500)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ─── Avatar / Robot ───────────────────────────────────────────────
            when {
                // ✅ Priority 1: Habbo avatar if username provided
                showHabboAvatar && habboUsername.isNotBlank() -> {
                    HabboAvatarBubble(
                        habboUsername = habboUsername,
                        habboRegion = habboRegion  // ✅ pass region
                    )
                }
                // ✅ Priority 2: Custom sprite image
                robotSpriteResId != null -> {
                    Image(
                        painter = painterResource(id = robotSpriteResId),
                        contentDescription = "Talking Robot",
                        modifier = Modifier.size(64.dp)
                    )
                }
                // ✅ Priority 3: Default SmartToy icon
                else -> {
                    DefaultRobotIcon()
                }
            }

            // ─── Speech Bubble ────────────────────────────────────────────────
            SpeechBubble(message = message)
        }
    }
}

// ─── Habbo Avatar Bubble ──────────────────────────────────────────────────────

@Composable
fun HabboAvatarBubble(
    habboUsername: String,
    habboRegion: String = "habbo.com"
) {
    val context = LocalContext.current
    Box(contentAlignment = Alignment.BottomCenter) {
        // ✅ Yellow shadow underneath — like a drop shadow in yellow
        Box(
            modifier = Modifier
                .size(92.dp)
                .offset(y = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ScrapbookYellow.copy(alpha = 0.25f),
                            ScrapbookYellow.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .blur(8.dp)
        )
        // ✅ Avatar — bigger, character is the star
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(habboAvatarUrl(habboUsername, habboRegion))
                .crossfade(true)
                .diskCachePolicy(CachePolicy.DISABLED)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .build(),
            contentDescription = "Habbo Avatar",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(96.dp)  // ✅ bigger
                .clip(RoundedCornerShape(10.dp)),
            error = rememberVectorPainter(image = Icons.Filled.SmartToy),
            placeholder = rememberVectorPainter(image = Icons.Filled.SmartToy)
        )
        // ✅ Tiny H badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(14.dp)
                .clip(CircleShape)
                .background(ScrapbookYellow.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "H",
                fontFamily = BangersFontFamily,
                color = ScrapbookDark,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─── Default Robot Icon ───────────────────────────────────────────────────────

@Composable
fun DefaultRobotIcon() {
    Box(
        modifier = Modifier
            .size(96.dp)  // ✅ match habbo size
            .clip(RoundedCornerShape(12.dp))
            .background(ScrapbookDark)
            .border(2.dp, ScrapbookYellow.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.SmartToy,
            contentDescription = "Talking Robot",
            modifier = Modifier.size(52.dp),  // ✅ bigger icon too
            tint = ScrapbookYellow
        )
    }
}

// ─── Speech Bubble ────────────────────────────────────────────────────────────

@Composable
fun SpeechBubble(message: String) {
    Box(
        modifier = Modifier
            .wrapContentWidth()
            .background(ScrapbookDark, RoundedCornerShape(12.dp))
            .border(
                width = 1.5.dp,
                color = ScrapbookYellow.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "RETRO BOT",
                fontFamily = BangersFontFamily,
                color = ScrapbookYellow,
                fontSize = 9.sp,
                letterSpacing = 1.sp
            )
            Text(
                text = message,
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                fontSize = 13.sp,
                textAlign = TextAlign.Start,
                lineHeight = 18.sp
            )
        }
    }
}

// ─── Habbo Avatar Card ────────────────────────────────────────────────────────

@Composable
fun HabboAvatarCard(
    habboUsername: String,
    habboRegion: String = "habbo.com",  // ✅ region param
    modifier: Modifier = Modifier,
    size: Int = 100
) {
    if (habboUsername.isBlank()) return
    val context = LocalContext.current

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ScrapbookDark)
            .border(2.dp, ScrapbookYellow, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.BottomCenter
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(habboAvatarUrl(habboUsername, habboRegion))  // ✅ use region
                .crossfade(true)
                .diskCachePolicy(CachePolicy.DISABLED)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .build(),
            contentDescription = "Habbo Avatar — $habboUsername",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp)),
            error = rememberVectorPainter(image = Icons.Filled.SmartToy),
            fallback = rememberVectorPainter(image = Icons.Filled.SmartToy)
        )
        // ✅ Username label at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ScrapbookDark.copy(alpha = 0.85f))
                .padding(vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = habboUsername,
                fontFamily = BangersFontFamily,
                color = ScrapbookYellow,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}
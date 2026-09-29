package com.example.hubretro

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.hubretro.ui.theme.BangersFontFamily
import com.example.hubretro.ui.theme.NunitoFontFamily
import com.example.hubretro.ui.theme.*
@Composable
fun MiniNowPlayingPlayer(
    nowPlayingViewModel: NowPlayingViewModel,
    onExpand: () -> Unit
) {
    val nowPlaying by nowPlayingViewModel.nowPlaying.collectAsState()
    val tracks by nowPlayingViewModel.tracks.collectAsState()
    val selectedTrack by nowPlayingViewModel.selectedTrack.collectAsState()
    val isPlaying by nowPlayingViewModel.isPlaying.collectAsState()

    if (nowPlaying == null) return

    val state = nowPlaying!!
    val scope = rememberCoroutineScope()

    // Controls visibility — auto-hide after 5 seconds
    var controlsVisible by remember { mutableStateOf(false) }
    var hideJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    fun showControlsTemporarily() {
        controlsVisible = true
        hideJob?.cancel()
        hideJob = scope.launch {
            delay(5000)
            controlsVisible = false
        }
    }

    // Era color for glow
    val eraColor = remember(nowPlaying?.albumId) {
        tracks.firstOrNull()?.let { CAcYellow } ?: VinylAmber
    }

    // Vinyl spin
    val spinT = rememberInfiniteTransition(label = "miniSpin")
    val spinAngle by spinT.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(if (isPlaying) 3000 else 10000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "miniSpinAngle"
    )

    // Ambient glow pulse
    val glowAlpha by rememberGlowRange(0.2f, 0.5f)

    // Track index info
    val currentIndex = tracks.indexOfFirst { it.id == selectedTrack?.id }
    val hasPrev = currentIndex > 0
    val hasNext = currentIndex < tracks.size - 1

    // Swipe gesture — left = next, right = prev
    var swipeOffset by remember { mutableStateOf(0f) }
    val animatedSwipeOffset by animateFloatAsState(
        targetValue = swipeOffset,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "swipeOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(10f)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // ── Ambient glow ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = 80.dp, height = 80.dp)
                .blur(24.dp)
                .background(VinylAmber.copy(alpha = glowAlpha), CircleShape)
        )

        // ── Controls panel — expands above vinyl ──────────────────────────────
        AnimatedVisibility(
            visible = controlsVisible,
            enter = slideInVertically(tween(300, easing = LinearOutSlowInEasing)) { it } +
                    fadeIn(tween(200)),
            exit = slideOutVertically(tween(250, easing = FastOutLinearInEasing)) { it } +
                    fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(VinylDeep, VinylSurface)
                        )
                    )
                    .border(2.5.dp, ScrapbookDark, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Scanline texture
                Canvas(modifier = Modifier.matchParentSize()) {
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.01f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += 3f
                    }
                }

                Column {
                    // Track progress bar
                    if (tracks.isNotEmpty() && currentIndex >= 0) {
                        val progress = (currentIndex + 1).toFloat() / tracks.size.toFloat()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(VinylGroove)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(VinylAmber, VinylOrange)
                                        )
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cover art thumbnail
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(VinylSurface)
                                .border(1.dp, VinylAmber.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        ) {
                            when {
                                state.coverResId != null -> Image(
                                    painter = painterResource(id = state.coverResId),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                state.coverUrl != null -> AsyncImage(
                                    model = state.coverUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.halftoneReveal(state.coverUrl).fillMaxSize()
                                )
                                else -> Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) { Text("♪", color = VinylAmber, fontSize = 18.sp) }
                            }
                        }

                        // Track info
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                selectedTrack?.title ?: state.title,
                                fontFamily = BangersFontFamily,
                                color = VinylCream,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (state.artist.isNotBlank()) {
                                    Text(
                                        state.artist,
                                        fontFamily = NunitoFontFamily,
                                        color = VinylCream.copy(alpha = 0.45f),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }
                                if (tracks.isNotEmpty() && currentIndex >= 0) {
                                    Text(
                                        "${currentIndex + 1}/${tracks.size}",
                                        fontFamily = BangersFontFamily,
                                        color = VinylAmber.copy(alpha = 0.5f),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }

                        // Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Previous
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (hasPrev) VinylSurface else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (hasPrev) VinylGroove else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable(enabled = hasPrev) {
                                        showControlsTemporarily()
                                        nowPlayingViewModel.skipPrevious()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = if (hasPrev) VinylCream.copy(alpha = 0.8f) else VinylCream.copy(alpha = 0.15f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Play/Pause
                            Box(
                                modifier = Modifier.size(44.dp)
                            ) {
                                // Glow
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .blur(10.dp)
                                        .background(
                                            VinylAmber.copy(alpha = glowAlpha * 0.6f),
                                            CircleShape
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(VinylAmber, VinylOrange)
                                            )
                                        )
                                        .border(2.dp, VinylAmberDim, CircleShape)
                                        .clickable {
                                            showControlsTemporarily()
                                            nowPlayingViewModel.playPause()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = VinylDark,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Next
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (hasNext) VinylSurface else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (hasNext) VinylGroove else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable(enabled = hasNext) {
                                        showControlsTemporarily()
                                        nowPlayingViewModel.skipNext()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.SkipNext,
                                    contentDescription = "Next",
                                    tint = if (hasNext) VinylCream.copy(alpha = 0.8f) else VinylCream.copy(alpha = 0.15f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Expand to full player
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(VinylSurface)
                                    .border(1.dp, VinylGroove, CircleShape)
                                    .clickable { onExpand() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.KeyboardArrowUp,
                                    contentDescription = "Expand",
                                    tint = VinylCream.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Swipe hint
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "← swipe to skip →",
                            fontFamily = NunitoFontFamily,
                            color = VinylCream.copy(alpha = 0.2f),
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        // ── Mini Vinyl disc ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = animatedSwipeOffset.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { showControlsTemporarily() },
                        onDragEnd = {
                            if (swipeOffset < -60f && hasNext) {
                                nowPlayingViewModel.skipNext()
                            } else if (swipeOffset > 60f && hasPrev) {
                                nowPlayingViewModel.skipPrevious()
                            }
                            swipeOffset = 0f
                        },
                        onDragCancel = { swipeOffset = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            swipeOffset = (swipeOffset + dragAmount * 0.3f).coerceIn(-80f, 80f)
                        }
                    )
                }
        ) {
            // Outer glow ring
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .blur(12.dp)
                    .background(VinylAmber.copy(alpha = glowAlpha * 0.6f), CircleShape)
            )

            // Vinyl disc
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(ScrapbookDark)
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                VinylAmber.copy(alpha = glowAlpha),
                                VinylAmber.copy(alpha = 0.2f),
                                VinylAmber.copy(alpha = glowAlpha)
                            )
                        ),
                        shape = CircleShape
                    )
                    .clickable { showControlsTemporarily() },
                contentAlignment = Alignment.Center
            ) {
                // Spinning vinyl grooves
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(spinAngle)
                ) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val outerR = size.width / 2f

                    // Vinyl grooves
                    for (i in 1..8) {
                        val r = outerR * (0.4f + i * 0.055f)
                        drawCircle(
                            color = Color.White.copy(alpha = 0.03f),
                            radius = r,
                            center = Offset(cx, cy),
                            style = Stroke(width = 0.8f)
                        )
                    }

                    // Era colored ring
                    drawCircle(
                        color = VinylAmber.copy(alpha = 0.4f),
                        radius = outerR * 0.42f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5f)
                    )

                    // Label area
                    drawCircle(
                        color = Color(0xFF2A1A08),
                        radius = outerR * 0.36f,
                        center = Offset(cx, cy)
                    )
                }

                // Cover art as label
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(spinAngle)
                        .clip(CircleShape)
                        .background(ComicGlassBg)
                ) {
                    when {
                        state.coverResId != null -> Image(
                            painter = painterResource(id = state.coverResId),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            alpha = 0.85f
                        )
                        state.coverUrl != null -> AsyncImage(
                            model = state.coverUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            alpha = 0.85f
                        )
                    }
                }

                // Center hole
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF050302))
                )
            }

            // Playing indicator dot
            if (isPlaying) {
                val dotT = rememberInfiniteTransition(label = "miniDot")
                val dotA by dotT.animateFloat(
                    initialValue = 0.4f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
                    label = "miniDotA"
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(VinylOrange.copy(alpha = dotA))
                        .border(1.dp, VinylDark, CircleShape)
                )
            }
        }
    }
}
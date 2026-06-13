package com.example.hubretro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hubretro.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun RetroRadioPlayer(
    radioViewModel: RetroRadioViewModel,
    modifier: Modifier = Modifier
) {
    val state by radioViewModel.radioState.collectAsState()

    val neonT = rememberInfiniteTransition(label = "radioNeon")
    val neonAlpha by neonT.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse),
        label = "radioNeonAlpha"
    )
    val dotAlpha by neonT.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "radioDotAlpha"
    )
    val emojiScale by neonT.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(600, easing = EaseInOut), RepeatMode.Reverse),
        label = "radioEmojiScale"
    )

    // ✅ Equalizer bars for playing state
    val eqT = rememberInfiniteTransition(label = "radioEq")
    val eqHeights = (0..4).map { i ->
        eqT.animateFloat(
            initialValue = 3f, targetValue = (12 + i * 2).toFloat(),
            animationSpec = infiniteRepeatable(tween(280 + i * 70, easing = EaseInOut), RepeatMode.Reverse),
            label = "radioEq_$i"
        )
    }

    val stationColor = state.currentStation?.color ?: ScrapbookYellow

    Column(modifier = modifier.fillMaxWidth()) {

        // ─── Expanded Panel ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible = state.isExpanded,
            enter = expandVertically(tween(350, easing = LinearOutSlowInEasing)),
            exit = shrinkVertically(tween(250))
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Neon glow behind panel
                Box(modifier = Modifier.matchParentSize().blur(12.dp).background(stationColor.copy(alpha = neonAlpha * 0.08f)))

                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(ScrapbookDark)
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    stationColor.copy(alpha = neonAlpha * 0.6f),
                                    stationColor.copy(alpha = 0.2f),
                                    stationColor.copy(alpha = neonAlpha * 0.6f)
                                )
                            ),
                            shape = RoundedCornerShape(0.dp)
                        )
                        .padding(bottom = 12.dp)
                ) {
                    // ─── Panel header ─────────────────────────────────────────
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        stationColor.copy(alpha = 0.12f),
                                        stationColor.copy(alpha = 0.05f),
                                        stationColor.copy(alpha = 0.12f)
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                        .background(stationColor.copy(alpha = 0.15f))
                                        .border(1.dp, stationColor.copy(alpha = neonAlpha * 0.5f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📻", fontSize = 16.sp)
                                }
                                Column {
                                    Text(
                                        "RETRO RADIO",
                                        fontFamily = BangersFontFamily,
                                        color = stationColor,
                                        fontSize = 16.sp,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        "${retroRadioStations.size} stations available",
                                        fontFamily = NunitoFontFamily,
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier.size(32.dp).clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .clickable { radioViewModel.collapse() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Collapse",
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = stationColor.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // ─── Currently playing ────────────────────────────────────
                    if (state.currentStation != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Station icon with glow
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier.size(62.dp).blur(10.dp)
                                        .background(stationColor.copy(alpha = neonAlpha * 0.3f), CircleShape)
                                )
                                Box(
                                    modifier = Modifier.size(56.dp).clip(CircleShape)
                                        .background(ScrapbookDark)
                                        .border(
                                            width = 2.dp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    stationColor.copy(alpha = neonAlpha),
                                                    stationColor.copy(alpha = 0.3f),
                                                    stationColor.copy(alpha = neonAlpha)
                                                )
                                            ),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (state.isLoading) {
                                        CircularProgressIndicator(
                                            color = stationColor,
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            text = state.currentStation!!.emoji,
                                            fontSize = if (state.isPlaying) (22 * emojiScale).sp else 22.sp
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    state.currentStation!!.name,
                                    fontFamily = BangersFontFamily,
                                    color = stationColor,
                                    fontSize = 18.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    state.currentStation!!.description,
                                    fontFamily = NunitoFontFamily,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    if (state.isPlaying) {
                                        Box(
                                            modifier = Modifier.size(7.dp).clip(CircleShape)
                                                .background(stationColor.copy(alpha = dotAlpha))
                                        )
                                    }
                                    Text(
                                        text = when {
                                            state.isLoading -> "⏳ Tuning in..."
                                            state.isPlaying -> "LIVE NOW"
                                            else -> "Paused"
                                        },
                                        fontFamily = BangersFontFamily,
                                        color = if (state.isPlaying) stationColor else Color.White.copy(alpha = 0.35f),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // ✅ Equalizer when playing
                            if (state.isPlaying) {
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.height(18.dp)
                                ) {
                                    eqHeights.forEachIndexed { i, heightState ->
                                        val h by heightState
                                        Box(
                                            modifier = Modifier.width(3.dp).height(h.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(stationColor.copy(alpha = 0.6f + i * 0.08f))
                                        )
                                    }
                                }
                            }

                            // Play / Pause button
                            if (!state.isLoading) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (state.isPlaying) {
                                        Box(
                                            modifier = Modifier.size(46.dp).blur(8.dp)
                                                .background(stationColor.copy(alpha = neonAlpha * 0.3f), CircleShape)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier.size(42.dp).clip(CircleShape)
                                            .background(if (state.isPlaying) stationColor else ScrapbookDark)
                                            .border(
                                                width = 2.dp,
                                                brush = Brush.linearGradient(
                                                    colors = listOf(
                                                        stationColor.copy(alpha = neonAlpha),
                                                        stationColor.copy(alpha = 0.3f),
                                                        stationColor.copy(alpha = neonAlpha)
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                if (state.isPlaying) radioViewModel.pause()
                                                else radioViewModel.resume()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = null,
                                            tint = if (state.isPlaying) ScrapbookDark else stationColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // ─── Error message ────────────────────────────────────────
                    state.error?.let { error ->
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ScrapbookRed.copy(alpha = 0.1f))
                                .border(1.dp, ScrapbookRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                "⚠️ $error",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = ScrapbookRed,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // ─── Station label ────────────────────────────────────────
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier.width(3.dp).height(14.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(ScrapbookYellow.copy(alpha = neonAlpha))
                        )
                        Text(
                            "SELECT A STATION",
                            fontFamily = BangersFontFamily,
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp,
                            letterSpacing = 2.sp
                        )
                    }

                    // ─── Station cards ────────────────────────────────────────
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(retroRadioStations, key = { it.id }) { station ->
                            val isCurrentStation = state.currentStation?.id == station.id
                            val isThisPlaying = isCurrentStation && state.isPlaying
                            var pressed by remember { mutableStateOf(false) }
                            val cardScale by animateFloatAsState(
                                targetValue = if (pressed) 0.93f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "stationCard_${station.id}"
                            )

                            Box(
                                modifier = Modifier.width(96.dp).scale(cardScale)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isCurrentStation) station.color.copy(alpha = 0.12f)
                                        else ScrapbookDark
                                    )
                                    .border(
                                        width = if (isCurrentStation) 2.dp else 1.dp,
                                        brush = if (isCurrentStation) Brush.linearGradient(
                                            colors = listOf(
                                                station.color.copy(alpha = neonAlpha),
                                                station.color.copy(alpha = 0.3f),
                                                station.color.copy(alpha = neonAlpha)
                                            )
                                        ) else Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.1f),
                                                Color.White.copy(alpha = 0.1f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { pressed = true; radioViewModel.playStation(station) }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        station.emoji,
                                        fontSize = if (isThisPlaying) (24 * emojiScale).sp else 24.sp
                                    )
                                    Text(
                                        station.name,
                                        fontFamily = BangersFontFamily,
                                        color = if (isCurrentStation) station.color else Color.White.copy(alpha = 0.7f),
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        station.description,
                                        fontFamily = NunitoFontFamily,
                                        color = Color.White.copy(alpha = 0.35f),
                                        fontSize = 8.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 11.sp
                                    )
                                    if (isCurrentStation && state.isLoading) {
                                        CircularProgressIndicator(
                                            color = station.color,
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 1.5.dp
                                        )
                                    } else if (isThisPlaying) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.size(5.dp).clip(CircleShape)
                                                    .background(station.color.copy(alpha = dotAlpha))
                                            )
                                            Text(
                                                "LIVE",
                                                fontFamily = BangersFontFamily,
                                                color = station.color,
                                                fontSize = 8.sp
                                            )
                                        }
                                    }
                                }
                            }
                            LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
                        }
                    }

                    // ─── Stop button ──────────────────────────────────────────
                    if (state.currentStation != null) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .clickable { radioViewModel.stop() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Stop", tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(14.dp))
                                Text("STOP RADIO", fontFamily = BangersFontFamily, color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // ─── Mini bubble ─────────────────────────────────────────────────────
        var showHint by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) { delay(5000L); showHint = false }
        LaunchedEffect(state.isPlaying) { if (state.isPlaying) { showHint = true; delay(5000L); showHint = false } }

        Box(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Box(contentAlignment = Alignment.BottomStart) {
                // Neon glow behind bubble
                if (state.isPlaying) {
                    Box(
                        modifier = Modifier.padding(start = 12.dp).clip(RoundedCornerShape(24.dp))
                            .blur(12.dp)
                            .background(stationColor.copy(alpha = neonAlpha * 0.25f))
                            .padding(horizontal = if (showHint) 14.dp else 10.dp, vertical = 8.dp)
                    )
                }
                Row(
                    modifier = Modifier.padding(start = 12.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(ScrapbookDark)
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    stationColor.copy(alpha = if (state.isPlaying) neonAlpha * 0.8f else 0.3f),
                                    stationColor.copy(alpha = if (state.isPlaying) neonAlpha * 0.4f else 0.1f),
                                    stationColor.copy(alpha = if (state.isPlaying) neonAlpha * 0.8f else 0.3f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable { radioViewModel.toggleExpanded() }
                        .padding(horizontal = if (showHint) 12.dp else 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Icon or loader
                    if (state.isLoading) {
                        CircularProgressIndicator(color = stationColor, modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp)
                    } else {
                        Text(
                            text = state.currentStation?.emoji ?: "📻",
                            fontSize = if (state.isPlaying) (14 * emojiScale).sp else 14.sp
                        )
                    }

                    // Hint text
                    AnimatedVisibility(visible = showHint, enter = fadeIn(tween(400)), exit = fadeOut(tween(400))) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Column {
                                Text(
                                    text = state.currentStation?.name ?: "RETRO RADIO",
                                    fontFamily = BangersFontFamily,
                                    color = if (state.currentStation != null) stationColor else Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = when {
                                        state.isPlaying -> "▶ Now playing"
                                        state.isLoading -> "Tuning in..."
                                        state.currentStation != null -> "Paused"
                                        else -> "Tap to open radio 🎵"
                                    },
                                    fontFamily = NunitoFontFamily,
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 9.sp
                                )
                            }

                            // ✅ Equalizer bars when playing
                            if (state.isPlaying) {
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.height(14.dp)
                                ) {
                                    eqHeights.take(3).forEachIndexed { i, heightState ->
                                        val h by heightState
                                        Box(
                                            modifier = Modifier.width(2.dp)
                                                .height((h * 0.7f).dp)
                                                .clip(RoundedCornerShape(1.dp))
                                                .background(stationColor.copy(alpha = 0.7f))
                                        )
                                    }
                                }
                            }

                            // Quick play/pause
                            if (state.currentStation != null && !state.isLoading) {
                                Box(
                                    modifier = Modifier.size(26.dp).clip(CircleShape)
                                        .background(stationColor.copy(alpha = 0.15f))
                                        .border(1.dp, stationColor.copy(alpha = 0.4f), CircleShape)
                                        .clickable {
                                            if (state.isPlaying) radioViewModel.pause()
                                            else radioViewModel.resume()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = stationColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
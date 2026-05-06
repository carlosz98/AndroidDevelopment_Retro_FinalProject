package com.example.hubretro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hubretro.ui.theme.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay

@Composable
fun RetroRadioPlayer(
    radioViewModel: RetroRadioViewModel,
    modifier: Modifier = Modifier
) {
    val state by radioViewModel.radioState.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "radio_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            tween(600, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "glow"
    )

    val stationColor = state.currentStation?.color ?: VaporwavePink

    Column(modifier = modifier.fillMaxWidth()) {

        // --- Expanded full player — slides up from mini bar ---
        AnimatedVisibility(
            visible = state.isExpanded,
            enter = slideInVertically { it } + fadeIn(tween(300)),
            exit = slideOutVertically { it } + fadeOut(tween(200))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0A18).copy(alpha = 0.98f))
                    .border(
                        1.dp,
                        stationColor.copy(alpha = 0.3f),
                        RoundedCornerShape(0.dp)
                    )
                    .padding(bottom = 8.dp)
            ) {
                // Drag handle + close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📻 RETRO RADIO",
                        fontFamily = RetroFontFamily,
                        color = stationColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        style = TextStyle(
                            shadow = Shadow(
                                color = stationColor.copy(alpha = 0.5f),
                                offset = Offset(1f, 1f),
                                blurRadius = 2f
                            )
                        )
                    )
                    IconButton(
                        onClick = { radioViewModel.collapse() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Collapse",
                            tint = RetroTextOffWhite.copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Divider(color = stationColor.copy(alpha = 0.2f))

                Spacer(modifier = Modifier.height(12.dp))

                // Currently playing info
                if (state.currentStation != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Big station emoji
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(stationColor.copy(alpha = 0.15f))
                                .border(
                                    2.dp,
                                    stationColor.copy(alpha = if (state.isPlaying) glowAlpha else 0.4f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    color = stationColor,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = state.currentStation!!.emoji,
                                    fontSize = if (state.isPlaying) (22 * pulseScale).sp else 22.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.currentStation!!.name,
                                fontFamily = RetroFontFamily,
                                color = stationColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = state.currentStation!!.description,
                                fontFamily = RetroFontFamily,
                                color = RetroTextOffWhite.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when {
                                    state.isLoading -> "⏳ Tuning in..."
                                    state.isPlaying -> "▶ LIVE — Now Playing"
                                    else -> "⏸ Paused"
                                },
                                fontFamily = RetroFontFamily,
                                color = if (state.isPlaying) stationColor
                                else RetroTextOffWhite.copy(alpha = 0.4f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Play / Pause big button
                        if (!state.isLoading) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(stationColor.copy(alpha = 0.2f))
                                    .border(1.dp, stationColor.copy(alpha = 0.6f), CircleShape)
                                    .clickable {
                                        if (state.isPlaying) radioViewModel.pause()
                                        else radioViewModel.resume()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (state.isPlaying)
                                        Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = stationColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Error message
                state.error?.let { error ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SynthwaveOrange.copy(alpha = 0.1f))
                            .border(1.dp, SynthwaveOrange.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "⚠️ $error",
                            fontFamily = RetroFontFamily,
                            color = SynthwaveOrange,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Station selector label
                Text(
                    text = "SELECT A STATION",
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Station cards
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(retroRadioStations, key = { it.id }) { station ->
                        val isCurrentStation = state.currentStation?.id == station.id
                        val isThisPlaying = isCurrentStation && state.isPlaying

                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isCurrentStation)
                                        station.color.copy(alpha = 0.2f)
                                    else
                                        Color(0xFF1A1A2E)
                                )
                                .border(
                                    width = if (isCurrentStation) 2.dp else 1.dp,
                                    color = if (isCurrentStation)
                                        station.color.copy(
                                            alpha = if (isThisPlaying) glowAlpha else 0.8f
                                        )
                                    else
                                        RetroTextOffWhite.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { radioViewModel.playStation(station) }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = station.emoji,
                                    fontSize = 26.sp
                                )
                                Text(
                                    text = station.name,
                                    fontFamily = RetroFontFamily,
                                    color = if (isCurrentStation) station.color
                                    else RetroTextOffWhite,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = station.description,
                                    fontFamily = RetroFontFamily,
                                    color = RetroTextOffWhite.copy(alpha = 0.4f),
                                    fontSize = 8.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 11.sp
                                )
                                if (isCurrentStation && state.isLoading) {
                                    CircularProgressIndicator(
                                        color = station.color,
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else if (isThisPlaying) {
                                    Text(
                                        text = "▶ LIVE",
                                        fontFamily = RetroFontFamily,
                                        color = station.color,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Stop button
                if (state.currentStation != null) {
                    TextButton(
                        onClick = { radioViewModel.stop() },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 4.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Stop",
                            tint = RetroTextOffWhite.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "STOP RADIO",
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite.copy(alpha = 0.4f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // --- Mini bar — always visible, tap to expand ---
        // --- Mini floating bubble ---
        var showHint by remember { mutableStateOf(true) }

        // Auto-hide hint text after 5 seconds
        LaunchedEffect(Unit) {
            delay(5000L)
            showHint = false
        }

        // Always show hint again when something is playing
        LaunchedEffect(state.isPlaying) {
            if (state.isPlaying) {
                showHint = true
                delay(5000L)
                showHint = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0A0A18).copy(alpha = 0.92f))
                    .border(
                        1.dp,
                        stationColor.copy(
                            alpha = if (state.isPlaying) glowAlpha * 0.8f else 0.35f
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .clickable { radioViewModel.toggleExpanded() }
                    .padding(
                        horizontal = if (showHint) 14.dp else 10.dp,
                        vertical = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Emoji icon — always visible
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = stationColor,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = state.currentStation?.emoji ?: "📻",
                        fontSize = if (state.isPlaying) (14 * pulseScale).sp else 14.sp
                    )
                }

                // Hint text — fades out after 5 seconds
                AnimatedVisibility(
                    visible = showHint,
                    enter = fadeIn(tween(400)),
                    exit = fadeOut(tween(400))
                ) {
                    Column {
                        Text(
                            text = state.currentStation?.name ?: "RETRO RADIO",
                            fontFamily = RetroFontFamily,
                            color = if (state.currentStation != null) stationColor
                            else RetroTextOffWhite,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                state.isPlaying -> "▶ Now playing"
                                state.isLoading -> "Tuning in..."
                                state.currentStation != null -> "Paused"
                                else -> "Tap to open radio 🎵"
                            },
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite.copy(alpha = 0.5f),
                            fontSize = 9.sp
                        )
                    }
                }

                // Quick play/pause — only when station selected and hint visible
                if (state.currentStation != null && !state.isLoading && showHint) {
                    IconButton(
                        onClick = {
                            if (state.isPlaying) radioViewModel.pause()
                            else radioViewModel.resume()
                        },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(stationColor.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying)
                                Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = stationColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
package com.example.hubretro

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.hubretro.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ═══════════════════════════════════════════════════════════════════════════════
// WARP TRANSITION — full-screen hyperspace "level load" between big moments:
//   account created → profile setup, setup done → main app, login → main app.
// Also used as the loading screen while a player's profile is being fetched.
// ═══════════════════════════════════════════════════════════════════════════════

object WarpTransitionBus {
    data class Warp(val title: String, val subtitle: String, val id: Long)
    var current by mutableStateOf<Warp?>(null)
        private set

    fun show(title: String, subtitle: String) { current = Warp(title, subtitle, System.nanoTime()) }
    fun finish(id: Long) { if (current?.id == id) current = null }
}

/** Put once near the app root. It's a Dialog, so it always covers whatever screen is behind. */
@Composable
fun WarpTransitionHost() {
    val warp = WarpTransitionBus.current ?: return
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        key(warp.id) {
            WarpScreen(
                title = warp.title,
                subtitle = warp.subtitle,
                durationMs = 2600,
                onFinished = { WarpTransitionBus.finish(warp.id) }
            )
        }
    }
}

/** Shown while a signed-in player's profile loads (instead of flashing the main page). */
@Composable
fun PlayerLoadingScreen(onRetry: () -> Unit) {
    var slow by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(9000); slow = true }
    Box(Modifier.fillMaxSize()) {
        WarpScreen(title = "LOADING PLAYER", subtitle = "Syncing your save file…", durationMs = null, onFinished = { })
        if (slow) {
            Text(
                "TAKING A WHILE? TAP TO RETRY",
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 40.dp)
                    .clip(RoundedCornerShape(20.dp)).background(Color.White)
                    .border(2.dp, ScrapbookDark, RoundedCornerShape(20.dp))
                    .clickable { slow = false; onRetry() }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                style = TextStyle(fontFamily = BangersFontFamily, fontSize = 15.sp, letterSpacing = 1.sp, color = CGreenDeep)
            )
        }
    }
}

private val WarpBgInner = Color(0xFF1B4332)
private val WarpBgOuter = Color(0xFF081C15)

private class Star(val angle: Float, val speed: Float, val start: Float, val width: Float, val mint: Boolean)

/**
 * The animation itself:
 *  1. stars stretch into hyperspace streaks (speed ramps up)
 *  2. the RETROHUB cartridge-sticker slams in, title types out
 *  3. 12-segment "NOW LOADING" bar fills with chiptune ticks
 *  4. white flash + zoom out → reveals the next screen
 * durationMs = null → loops (used as a loading screen).
 */
@Composable
fun WarpScreen(title: String, subtitle: String, durationMs: Int?, onFinished: () -> Unit) {
    val stars = remember {
        val r = Random(42)
        List(110) { Star(r.nextFloat() * 6.2832f, 0.35f + r.nextFloat() * 0.9f, r.nextFloat(), 1f + r.nextFloat() * 2.5f, r.nextFloat() < 0.35f) }
    }
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameMillis { it }
        while (isActive) withFrameMillis { time = (it - start) / 1000f }
    }

    val logoScale = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }
    val bar = remember { Animatable(0f) }
    var typed by remember { mutableIntStateOf(0) }
    val haptic = rememberComicHaptic()

    LaunchedEffect(title) {
        Chiptune.play(Chiptune.Sfx.POWER_UP)
        launch { logoScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 380f)) }
        launch {
            delay(250)
            for (i in 1..title.length) { typed = i; delay(38) }
        }
        if (durationMs != null) {
            val loadMs = (durationMs - 700).coerceAtLeast(600)
            launch {
                var lastSeg = 0
                bar.animateTo(1f, tween(loadMs, easing = FastOutSlowInEasing)) {
                    val seg = (value * 12).toInt()
                    if (seg != lastSeg && seg % 3 == 0) Chiptune.play(Chiptune.Sfx.BLIP)
                    lastSeg = seg
                }
            }
            delay(loadMs.toLong() + 100)
            haptic()
            Chiptune.play(Chiptune.Sfx.COIN)
            launch { flash.animateTo(1f, tween(140)); flash.animateTo(0f, tween(360)) }
            exit.animateTo(1f, tween(460, easing = FastOutLinearInEasing))
            onFinished()
        } else {
            // looping loader
            while (isActive) {
                bar.snapTo(0f)
                bar.animateTo(1f, tween(1800, easing = LinearEasing))
                delay(200)
            }
        }
    }

    val blink by rememberInfiniteTransition(label = "warpBlink").animateFloat(
        0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "warpBlinkA"
    )

    Box(
        modifier = Modifier.fillMaxSize()
            .graphicsLayer {
                val e = exit.value
                scaleX = 1f + e * 0.25f; scaleY = 1f + e * 0.25f
                alpha = 1f - e
            }
            .background(Brush.radialGradient(listOf(WarpBgInner, WarpBgOuter))),
        contentAlignment = Alignment.Center
    ) {
        // ── Hyperspace streaks ──
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.maxDimension * 0.75f
            val accel = (0.6f + time * 0.9f).coerceAtMost(3.2f) + exit.value * 4f
            stars.forEach { s ->
                val p = ((s.start + time * s.speed * accel * 0.35f) % 1f)
                val d = p * p * maxR
                val tail = (0.05f + 0.25f * p) * maxR * (accel / 3.2f)
                val dx = cos(s.angle); val dy = sin(s.angle)
                val a = Offset(c.x + dx * d, c.y + dy * d)
                val b = Offset(c.x + dx * (d - tail).coerceAtLeast(0f), c.y + dy * (d - tail).coerceAtLeast(0f))
                drawLine(
                    color = (if (s.mint) CGreenMint else Color.White).copy(alpha = (0.25f + p * 0.75f)),
                    start = b, end = a, strokeWidth = s.width * (0.6f + p * 1.6f), cap = StrokeCap.Round
                )
            }
            // scanlines
            var y = 0f
            val step = 4.dp.toPx()
            while (y < size.height) { drawLine(Color.Black.copy(alpha = 0.10f), Offset(0f, y), Offset(size.width, y), 1f); y += step }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
            // ── Cartridge sticker logo ──
            Box(modifier = Modifier.graphicsLayer {
                scaleX = logoScale.value; scaleY = logoScale.value
                rotationZ = (1f - logoScale.value) * -14f + sin(time * 2f) * 1.5f
            }) {
                Box(Modifier.matchParentSize().offset(6.dp, 6.dp).clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = 0.55f)))
                Column(
                    modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(CGreen)
                        .border(3.dp, ScrapbookDark, RoundedCornerShape(18.dp))
                        .padding(horizontal = 26.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(7) { Box(Modifier.size(width = 6.dp, height = 4.dp).background(ScrapbookDark.copy(alpha = 0.35f))) }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("RETROHUB", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 40.sp, letterSpacing = 4.sp, color = Color.White))
                }
            }
            Spacer(Modifier.height(28.dp))

            // ── Typed title ──
            Text(
                title.take(typed) + if (typed < title.length) "▌" else "",
                textAlign = TextAlign.Center,
                style = TextStyle(fontFamily = BangersFontFamily, fontSize = 34.sp, letterSpacing = 2.sp, color = Color.White, lineHeight = 38.sp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle, textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = (typed.toFloat() / title.length.coerceAtLeast(1)).coerceIn(0f, 1f) },
                style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = CGreenMint)
            )
            Spacer(Modifier.height(30.dp))

            // ── 12-segment loading bar ──
            Row(
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.35f))
                    .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(6.dp)).padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val filled = (bar.value * 12).toInt()
                repeat(12) { i ->
                    Box(
                        Modifier.size(width = 14.dp, height = 16.dp).clip(RoundedCornerShape(2.dp))
                            .background(if (i < filled) CGreenMint else Color.White.copy(alpha = 0.08f))
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (bar.value >= 1f && durationMs != null) "PRESS START" else "NOW LOADING",
                modifier = Modifier.graphicsLayer { alpha = if (blink < 0.6f) 1f else 0.2f },
                style = TextStyle(fontFamily = BangersFontFamily, fontSize = 16.sp, letterSpacing = 3.sp, color = Color.White)
            )
        }

        // white flash on exit
        if (flash.value > 0f) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = flash.value)))
    }
}

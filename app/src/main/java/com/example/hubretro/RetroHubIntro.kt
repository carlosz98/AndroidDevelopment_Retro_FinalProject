package com.example.hubretro

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hubretro.ui.theme.BangersFontFamily
import com.example.hubretro.ui.theme.CGreen
import com.example.hubretro.ui.theme.CGreenDeep
import com.example.hubretro.ui.theme.ComicGlassBg
import com.example.hubretro.ui.theme.NunitoFontFamily
import com.example.hubretro.ui.theme.ScrapbookBorder
import com.example.hubretro.ui.theme.ScrapbookCardWhite
import com.example.hubretro.ui.theme.ScrapbookYellow
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan
import kotlin.random.Random

// ─── Launch intro ─────────────────────────────────────────────────────────────
// Comic "slam" splash in the app's green scrapbook palette. Plays once per fresh
// launch (~3.6s), tap anywhere / system back to skip, skipped when the device's
// animation scale is 0. Draws above every screen, including the auth gate.

// Palette — all pulled from ui/theme/Color.kt so a theme change carries through.
private val IntroGreen = CGreen                 // 0xFF52B788
private val IntroGreenDeep = CGreenDeep         // 0xFF40916C
private val IntroGreenLight = ScrapbookYellow   // pale mint, 0xFFB7E4C7 in this theme
private val IntroMint = ComicGlassBg            // 0xFFEFF7F2
private val IntroCard = ScrapbookCardWhite
private val IntroInk = ScrapbookBorder

// Timeline (seconds)
private const val INTRO_TOTAL = 3.6f

private fun introMix(a: Float, b: Float, p: Float) = a + (b - a) * p

private fun introSeg(t: Float, start: Float, dur: Float, easing: Easing = LinearEasing): Float =
    easing.transform(((t - start) / dur).coerceIn(0f, 1f))

private val IntroBackOut = Easing { x ->
    val c1 = 2.2f
    val c3 = c1 + 1f
    1f + c3 * (x - 1f).pow(3) + c1 * (x - 1f).pow(2)
}
private val IntroSlam = CubicBezierEasing(0.2f, 1.3f, 0.4f, 1f)
private val IntroEaseOutStrong = CubicBezierEasing(0.1f, 0.9f, 0.3f, 1f)
private val IntroWipeEase = CubicBezierEasing(0.6f, 0f, 0.4f, 1f)

private fun Modifier.introHardShadow(offset: Dp, radius: Dp, color: Color = IntroInk): Modifier =
    this.drawBehind {
        drawRoundRect(
            color = color,
            topLeft = Offset(offset.toPx(), offset.toPx()),
            size = size,
            cornerRadius = CornerRadius(radius.toPx())
        )
    }

private class IntroConfetto(
    val angle: Float,
    val dist: Float,
    val size: Float,
    val color: Color,
    val round: Boolean,
    val spin: Float
)

@Composable
fun RetroHubIntro(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    onImpact: () -> Unit = {}
) {
    val context = LocalContext.current
    val clock = remember { Animatable(0f) }
    var done by remember { mutableStateOf(false) }
    val finish: () -> Unit = {
        if (!done) {
            done = true
            onFinished()
        }
    }

    BackHandler(enabled = !done) { finish() }

    LaunchedEffect(Unit) {
        val scale = Settings.Global.getFloat(
            context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        )
        if (scale == 0f) {
            finish()
            return@LaunchedEffect
        }
        clock.animateTo(INTRO_TOTAL, tween((INTRO_TOTAL * 1000).toInt(), easing = LinearEasing))
        finish()
    }
    LaunchedEffect(Unit) {
        delay(620)
        onImpact()
    }

    val confetti = remember {
        val r = Random(7)
        val colors = listOf(IntroGreen, IntroGreenLight, IntroGreenDeep, IntroCard, IntroInk, IntroGreen)
        List(22) { i ->
            IntroConfetto(
                angle = i / 22f * 2f * PI.toFloat() + r.nextFloat() * 0.3f,
                dist = 130f + r.nextFloat() * 80f,
                size = 8f + r.nextInt(8),
                color = colors[i % 6],
                round = i % 3 == 0,
                spin = r.nextFloat() * 360f
            )
        }
    }

    val t = clock.value

    // Pin font scale so big accessibility text sizes can't break the comic layout.
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
        BoxWithConstraints(
            modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 1f - introSeg(t, INTRO_TOTAL - 0.3f, 0.3f) }
                .background(IntroMint)
                .pointerInput(Unit) { detectTapGestures { finish() } }
        ) {
            val w = maxWidth
            val h = maxHeight

            // Drifting dot grid
            Canvas(Modifier.fillMaxSize()) {
                val step = 14.dp.toPx()
                val drift = (t * 14f / 6f).dp.toPx() % step
                var y = -step + drift
                while (y < size.height + step) {
                    var x = -step + drift
                    while (x < size.width + step) {
                        drawCircle(IntroInk.copy(alpha = 0.13f), 1.4.dp.toPx(), Offset(x, y))
                        x += step
                    }
                    y += step
                }
            }

            // Stage (shakes on impact)
            val sp = introSeg(t, 0.62f, 0.4f)
            val shakeAmp = if (sp > 0f && sp < 1f) 1f - sp else 0f
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = sin(sp * 22f) * 7.dp.toPx() * shakeAmp
                        translationY = cos(sp * 17f) * 5.dp.toPx() * shakeAmp
                    }
            ) {
                // Starburst
                val bp = introSeg(t, 0.33f, 0.55f, IntroBackOut)
                val spin = if (t > 2.3f) (t - 2.3f) * (360f / 14f) else 0f
                Canvas(
                    Modifier
                        .align(Alignment.Center)
                        .size(330.dp)
                        .graphicsLayer {
                            val s = introMix(0.1f, 1f, bp)
                            scaleX = s
                            scaleY = s
                            rotationZ = introMix(-30f, 0f, bp) + spin
                            alpha = if (t >= 0.33f) 1f else 0f
                        }
                ) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val k = size.width / 330f
                    val path = Path()
                    for (i in 0 until 32) {
                        val r = (if (i % 2 == 1) 112f else 158f) * k
                        val a = i / 32f * 2f * PI.toFloat()
                        val px = c.x + cos(a) * r
                        val py = c.y + sin(a) * r
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    path.close()
                    drawPath(path, IntroGreenLight)
                    drawPath(path, IntroInk, style = Stroke(3.dp.toPx(), join = StrokeJoin.Round))
                }

                // Shockwave rings + speed lines
                Canvas(Modifier.fillMaxSize()) {
                    val c = center
                    fun ring(delay: Float, color: Color) {
                        val p = introSeg(t, 0.62f + delay, 0.75f, LinearOutSlowInEasing)
                        if (p > 0f && p < 1f) {
                            drawCircle(
                                color = color.copy(alpha = 1f - p),
                                radius = 60.dp.toPx() * introMix(0.3f, 3.4f, p),
                                center = c,
                                style = Stroke(4.dp.toPx())
                            )
                        }
                    }
                    ring(0f, IntroInk)
                    ring(0.12f, IntroGreenDeep)

                    val lp = introSeg(t, 0.62f, 0.5f, LinearOutSlowInEasing)
                    if (lp > 0f && lp < 1f) {
                        val s = introMix(0.6f, 1.5f, lp)
                        for (i in 0 until 26) {
                            val a = i / 26f * 2f * PI.toFloat()
                            val dir = Offset(cos(a), sin(a))
                            val r1 = 125.dp.toPx() * s
                            val r2 = (if (i % 2 == 1) 185f else 215f).dp.toPx() * s
                            drawLine(
                                color = IntroInk.copy(alpha = 1f - lp),
                                start = c + dir * r1,
                                end = c + dir * r2,
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                // Stickers
                val icons = listOf(Icons.Filled.Star, Icons.Filled.Favorite, Icons.Filled.ShoppingCart, Icons.Filled.Notifications)
                val fills = listOf(IntroGreenLight, IntroGreen, IntroCard, IntroGreenLight)
                val xs = listOf(0.08f, 0.84f, 0.09f, 0.85f)
                val ys = listOf(0.10f, 0.14f, 0.80f, 0.82f)
                val rots = listOf(-12f, 10f, 8f, -9f)
                for (k in 0 until 4) {
                    val s = introSeg(t, 1.75f + k * 0.12f, 0.45f, IntroBackOut)
                    val wig = if (t > 2.3f) sin((t - 2.3f) * 4f + k) * 4f else 0f
                    IntroSticker(
                        icon = icons[k],
                        fill = fills[k],
                        modifier = Modifier
                            .offset(x = w * xs[k], y = h * ys[k])
                            .graphicsLayer {
                                scaleX = s
                                scaleY = s
                                rotationZ = rots[k] + wig
                            }
                    )
                }

                // Sparkles
                val sx = listOf(0.22f, 0.74f, 0.14f, 0.84f, 0.28f, 0.70f)
                val sy = listOf(0.28f, 0.24f, 0.52f, 0.50f, 0.72f, 0.76f)
                for (k in 0 until 6) {
                    val v = if (t > 1.75f) (sin((t - 1.75f) * 4.8f + k * 1.3f) + 1f) / 2f else 0f
                    IntroSparkle(
                        Modifier
                            .offset(x = w * sx[k] - 11.dp, y = h * sy[k] - 11.dp)
                            .graphicsLayer {
                                val s = 0.15f + v * 0.95f
                                scaleX = s
                                scaleY = s
                                rotationZ = v * 90f
                                alpha = if (t > 1.75f) 1f else 0f
                            }
                    )
                }

                // Card
                val slam = introSeg(t, 0.33f, 0.35f, IntroSlam)
                val bob = if (t > 2.3f) sin((t - 2.3f) * 2f * PI.toFloat() / 2.2f) * -2.5f else 0f
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            val s = introMix(1.7f, 1f, slam)
                            scaleX = s
                            scaleY = s
                            rotationZ = introMix(7f, -2f, slam)
                            alpha = if (t >= 0.33f) 1f else 0f
                            translationY = bob.dp.toPx()
                        }
                ) {
                    IntroCardContent(t)

                    val tp = introSeg(t, 0.65f, 0.5f)
                    Box(
                        Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (-14).dp, y = (-12).dp)
                            .size(56.dp, 22.dp)
                            .graphicsLayer {
                                rotationZ = -12f + (1f - tp) * 20f
                                translationY = -(1f - tp) * 14.dp.toPx()
                                alpha = if (t >= 0.62f) 0.95f else 0f
                            }
                            .background(IntroGreenLight)
                            .border(2.dp, IntroInk)
                    )
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 14.dp, y = (-12).dp)
                            .size(56.dp, 22.dp)
                            .graphicsLayer {
                                rotationZ = 9f - (1f - tp) * 20f
                                translationY = -(1f - tp) * 14.dp.toPx()
                                alpha = if (t >= 0.62f) 0.95f else 0f
                            }
                            .background(IntroGreenLight)
                            .border(2.dp, IntroInk)
                    )
                }

                // Confetti
                Canvas(Modifier.fillMaxSize()) {
                    if (t >= 0.62f) {
                        val cp = introSeg(t, 0.62f, 0.8f, IntroEaseOutStrong)
                        val fade = 1f - introSeg(t, 2.8f, 0.5f)
                        confetti.forEach { c ->
                            val pos = center + Offset(cos(c.angle), sin(c.angle) * 1.1f) * (c.dist.dp.toPx() * cp)
                            val s = c.size.dp.toPx() * cp
                            withTransform({ rotate(c.spin * cp, pos) }) {
                                if (c.round) {
                                    drawCircle(c.color.copy(alpha = fade), s / 2f, pos)
                                    drawCircle(IntroInk.copy(alpha = fade), s / 2f, pos, style = Stroke(2.dp.toPx()))
                                } else {
                                    val tl = Offset(pos.x - s / 2f, pos.y - s / 2f)
                                    drawRect(c.color.copy(alpha = fade), tl, Size(s, s))
                                    drawRect(IntroInk.copy(alpha = fade), tl, Size(s, s), style = Stroke(2.dp.toPx()))
                                }
                            }
                        }
                    }
                }

                // Press start (blinks until the intro hands off)
                if (t > 2.6f) {
                    val blink = if (((t - 2.6f) * 2f).toInt() % 2 == 0) 1f else 0f
                    Text(
                        text = "PRESS START",
                        fontFamily = BangersFontFamily,
                        fontSize = 22.sp,
                        letterSpacing = 5.sp,
                        color = IntroInk,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = h * 0.05f)
                            .graphicsLayer { alpha = blink }
                    )
                }
            }

            // Opening wipe
            Canvas(Modifier.fillMaxSize()) {
                val p = introSeg(t, 0f, 0.7f, IntroWipeEase)
                if (p > 0f && p < 1f) {
                    val bw = size.width * 0.5f
                    val sk = size.height * tan(14f * PI.toFloat() / 180f)
                    val x = introMix(-size.width * 0.6f, size.width * 1.3f, p)
                    val path = Path().apply {
                        moveTo(x + sk, 0f)
                        lineTo(x + sk + bw, 0f)
                        lineTo(x + bw, size.height)
                        lineTo(x, size.height)
                        close()
                    }
                    drawPath(path, IntroGreen)
                    drawLine(IntroInk, Offset(x + sk, 0f), Offset(x, size.height), 6.dp.toPx())
                    drawLine(IntroInk, Offset(x + sk + bw, 0f), Offset(x + bw, size.height), 6.dp.toPx())
                }
            }
        }
    }
}

@Composable
private fun IntroCardContent(t: Float) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .introHardShadow(6.dp, 16.dp)
            .background(IntroCard, shape)
            .border(3.dp, IntroInk, shape)
            .drawBehind {
                val step = 10.dp.toPx()
                var y = step / 2f
                while (y < size.height) {
                    var x = step / 2f
                    while (x < size.width) {
                        drawCircle(IntroInk.copy(alpha = 0.07f), 1.dp.toPx(), Offset(x, y))
                        x += step
                    }
                    y += step
                }
            }
            .padding(horizontal = 30.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Controller tile
        val tileP = introSeg(t, 0.8f, 0.45f, IntroBackOut)
        val tileShape = RoundedCornerShape(14.dp)
        Box(
            Modifier
                .size(58.dp)
                .graphicsLayer {
                    scaleX = tileP
                    scaleY = tileP
                    rotationZ = introMix(-40f, 0f, tileP) +
                        (if (t > 2.3f) sin((t - 2.3f) * 5f) * 6f else 0f)
                }
                .introHardShadow(3.dp, 14.dp)
                .background(IntroGreen, tileShape)
                .border(3.dp, IntroInk, tileShape),
            contentAlignment = Alignment.Center
        ) {
            IntroControllerGlyph()
        }

        // Title letters, stamped in one by one, then a shine sweeps across
        Row(
            Modifier
                .padding(top = 8.dp)
                .clipToBounds()
                .drawWithContent {
                    drawContent()
                    val p = introSeg(t, 1.75f, 0.7f, FastOutSlowInEasing)
                    if (p > 0f && p < 1f) {
                        val band = 22.dp.toPx()
                        val x = introMix(-40.dp.toPx(), size.width + 10.dp.toPx(), p)
                        val sk = size.height * tan(20f * PI.toFloat() / 180f)
                        val path = Path().apply {
                            moveTo(x + sk, 0f)
                            lineTo(x + sk + band, 0f)
                            lineTo(x + band, size.height)
                            lineTo(x, size.height)
                            close()
                        }
                        drawPath(path, IntroGreenLight.copy(alpha = 0.85f))
                    }
                }
                .padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            "RETROHUB".forEachIndexed { i, ch ->
                val lp = introSeg(t, 1.0f + i * 0.05f, 0.35f, IntroSlam)
                val hop = introSeg(t, 1.75f + i * 0.04f, 0.5f)
                val q = (if (i % 2 == 1) 1f else -1f) * (8f + i)
                Text(
                    text = ch.toString(),
                    fontFamily = BangersFontFamily,
                    fontSize = 48.sp,
                    letterSpacing = 3.sp,
                    color = IntroInk,
                    modifier = Modifier.graphicsLayer {
                        alpha = if (lp > 0f) 1f else 0f
                        translationY = introMix(34.dp.toPx(), 0f, lp) - 9.dp.toPx() * sin(PI.toFloat() * hop)
                        val s = introMix(1.5f, 1f, lp)
                        scaleX = s
                        scaleY = s
                        rotationZ = introMix(q, 0f, lp) - 3f * sin(PI.toFloat() * hop)
                    }
                )
            }
        }

        // Underline
        val lineP = introSeg(t, 1.55f, 0.4f, LinearOutSlowInEasing)
        Box(
            Modifier
                .padding(top = 2.dp)
                .width((160f * lineP).dp)
                .height(7.dp)
                .background(IntroGreenLight, RoundedCornerShape(4.dp))
                .border(2.dp, IntroInk, RoundedCornerShape(4.dp))
        )

        // Glass pill
        val pp = introSeg(t, 1.55f, 0.45f, IntroBackOut)
        Box(
            Modifier
                .padding(top = 16.dp)
                .graphicsLayer {
                    alpha = if (t > 1.55f) 1f else 0f
                    translationY = introMix(18.dp.toPx(), 0f, pp)
                    val s = introMix(0.8f, 1f, pp)
                    scaleX = s
                    scaleY = s
                }
                .clip(CircleShape)
                .background(IntroGreenLight)
                .drawBehind {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.45f),
                        topLeft = Offset(4.dp.toPx(), 3.dp.toPx()),
                        size = Size(size.width - 8.dp.toPx(), size.height * 0.42f),
                        cornerRadius = CornerRadius(100f)
                    )
                }
                .border(2.dp, IntroInk, CircleShape)
                .drawWithContent {
                    drawContent()
                    val p = introSeg(t, 2.3f, 0.6f, FastOutSlowInEasing)
                    if (p > 0f && p < 1f) {
                        val band = 18.dp.toPx()
                        val x = introMix(-30.dp.toPx(), size.width + 10.dp.toPx(), p)
                        val sk = size.height * tan(20f * PI.toFloat() / 180f)
                        val path = Path().apply {
                            moveTo(x + sk, 0f)
                            lineTo(x + sk + band, 0f)
                            lineTo(x + band, size.height)
                            lineTo(x, size.height)
                            close()
                        }
                        drawPath(path, Color.White.copy(alpha = 0.6f))
                    }
                }
                .padding(horizontal = 22.dp, vertical = 7.dp)
        ) {
            Text(
                text = "WELCOME",
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 4.sp,
                color = IntroInk,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun IntroControllerGlyph() {
    Canvas(Modifier.size(38.dp)) {
        val w = size.width
        val h = size.height
        val bodyTop = h * 0.28f
        val bodyH = h * 0.46f
        drawRoundRect(IntroCard, Offset(0f, bodyTop), Size(w, bodyH), CornerRadius(bodyH / 2f))
        drawRoundRect(
            IntroInk, Offset(0f, bodyTop), Size(w, bodyH), CornerRadius(bodyH / 2f),
            style = Stroke(2.5.dp.toPx())
        )
        val cx = w * 0.28f
        val cy = bodyTop + bodyH / 2f
        val a = w * 0.09f
        val l = w * 0.05f
        drawRect(IntroInk, Offset(cx - a, cy - l / 2f), Size(2f * a, l))
        drawRect(IntroInk, Offset(cx - l / 2f, cy - a), Size(l, 2f * a))
        drawCircle(IntroGreenDeep, w * 0.065f, Offset(w * 0.66f, cy - h * 0.03f))
        drawCircle(IntroGreen, w * 0.065f, Offset(w * 0.78f, cy + h * 0.05f))
    }
}

@Composable
private fun IntroSticker(icon: ImageVector, fill: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(46.dp)
            .introHardShadow(3.dp, 23.dp)
            .background(fill, CircleShape)
            .border(3.dp, IntroInk, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = IntroInk, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun IntroSparkle(modifier: Modifier = Modifier) {
    Canvas(modifier.size(22.dp)) {
        val c = center
        val k = size.width / 24f
        val pts = listOf(
            0f to -10f, 2.5f to -2.5f, 10f to 0f, 2.5f to 2.5f,
            0f to 10f, -2.5f to 2.5f, -10f to 0f, -2.5f to -2.5f
        )
        val path = Path()
        pts.forEachIndexed { i, p ->
            val x = c.x + p.first * k
            val y = c.y + p.second * k
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, IntroCard)
        drawPath(path, IntroInk, style = Stroke(1.5.dp.toPx(), join = StrokeJoin.Round))
    }
}

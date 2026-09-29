package com.example.hubretro

import android.content.Context
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.hubretro.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// ═══════════════════════════════════════════════════════════════════════════════
// 1. SHARED GLOW CLOCK
// One infinite animation for the whole app instead of one per card.
// ═══════════════════════════════════════════════════════════════════════════════

private val FallbackGlowPhase: State<Float> = mutableFloatStateOf(0.5f)

val LocalGlowPhase = staticCompositionLocalOf<State<Float>> { FallbackGlowPhase }

@Composable
fun ProvideGlowClock(content: @Composable () -> Unit) {
    val t = rememberInfiniteTransition(label = "globalGlowClock")
    val phase = t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOut), RepeatMode.Reverse),
        label = "globalGlowPhase"
    )
    CompositionLocalProvider(LocalGlowPhase provides phase, content = content)
}

/** Glow value from 0 to [max], driven by the shared app clock. Use: `val glowAlpha by rememberGlowPhase(0.4f)` */
@Composable
fun rememberGlowPhase(max: Float): State<Float> = rememberGlowRange(0f, max)

/** Pulses between [from] and [to] on the shared app clock. */
@Composable
fun rememberGlowRange(from: Float, to: Float): State<Float> {
    val phase = LocalGlowPhase.current
    return remember(phase, from, to) { derivedStateOf { from + phase.value * (to - from) } }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 2. CACHED HALFTONE
// Draws one tiny dot tile once, then repeats it as a shader — no per-frame loops.
// ═══════════════════════════════════════════════════════════════════════════════

private val halftoneTileCache = HashMap<String, ImageBitmap>()

private fun halftoneTile(spacingPx: Int, radiusPx: Float, color: Color): ImageBitmap {
    val key = "$spacingPx|$radiusPx|${color.value}"
    return halftoneTileCache.getOrPut(key) {
        val bmp = ImageBitmap(spacingPx, spacingPx)
        val canvas = androidx.compose.ui.graphics.Canvas(bmp)
        val paint = Paint().apply { this.color = color; isAntiAlias = true }
        val c = spacingPx / 2f
        canvas.drawCircle(Offset(c, c), radiusPx, paint)
        bmp
    }
}

@Composable
fun HalftoneDots(
    modifier: Modifier = Modifier,
    spacing: Dp = 6.dp,
    dotRadius: Dp = 1.3.dp,
    color: Color = Color.Black.copy(alpha = 0.13f)
) {
    Spacer(modifier.drawWithCache {
        val sp = spacing.toPx().roundToInt().coerceAtLeast(2)
        val tile = halftoneTile(sp, dotRadius.toPx(), color)
        val brush = ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
        onDrawBehind { drawRect(brush) }
    })
}

// ═══════════════════════════════════════════════════════════════════════════════
// 3. HAPTICS
// ═══════════════════════════════════════════════════════════════════════════════

/** Strong "confirm" haptic for big moments (bursts, level up). */
@Composable
fun rememberComicHaptic(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        {
            val c = if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM
                    else HapticFeedbackConstants.LONG_PRESS
            view.performHapticFeedback(c)
        }
    }
}

/** Light tick for ordinary taps. */
@Composable
fun rememberTapHaptic(): () -> Unit {
    val view = LocalView.current
    return remember(view) { { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 4. COMIC BURST — POW! ZAP! BAM! with pixel particles
// Usage:
//   val burst = rememberBurstState()
//   Box { MyButton(onClick = { burst.fire("POW!") }); ComicBurst(burst, Modifier.align(Alignment.Center)) }
// ═══════════════════════════════════════════════════════════════════════════════

val comicBurstWords = listOf("POW!", "ZAP!", "BAM!", "WHAM!", "BOOM!", "KAPOW!")

class BurstState {
    internal var trigger by mutableIntStateOf(0)
    internal var text by mutableStateOf("POW!")
    internal var color by mutableStateOf(CAcYellowL)

    fun fire(text: String = comicBurstWords.random(), color: Color = CAcYellowL) {
        this.text = text
        this.color = color
        trigger++
    }
}

@Composable
fun rememberBurstState(): BurstState = remember { BurstState() }

private fun starburstPath(cx: Float, cy: Float, outer: Float, inner: Float, points: Int, seed: Int): Path {
    val rnd = Random(seed)
    val path = Path()
    val total = points * 2
    for (i in 0 until total) {
        val angle = (Math.PI * 2 * i / total) - Math.PI / 2
        val r = if (i % 2 == 0) outer * (0.85f + rnd.nextFloat() * 0.15f) else inner
        val x = cx + (cos(angle) * r).toFloat()
        val y = cy + (sin(angle) * r).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private val particleColors = listOf(CGreen, CGreenMint, CGreenDeep, ScrapbookDark, Color.White)

@Composable
fun ComicBurst(state: BurstState, modifier: Modifier = Modifier, burstSize: Dp = 104.dp) {
    val haptic = rememberComicHaptic()
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    val spin = remember { Animatable(0f) }
    val particles = remember { Animatable(0f) }

    LaunchedEffect(state.trigger) {
        if (state.trigger == 0) return@LaunchedEffect
        haptic()
        Chiptune.play(Chiptune.Sfx.POP)
        if (Random.nextInt(4) == 0) RobotBrain.notify(RobotTrigger.HypeMoment)
        scale.snapTo(0.2f); alpha.snapTo(1f); particles.snapTo(0f); spin.snapTo(-20f)
        launch { scale.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 500f)) }
        launch { spin.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 300f)) }
        launch { particles.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        delay(560)
        alpha.animateTo(0f, tween(220))
    }

    if (alpha.value <= 0f) return
    val seed = state.trigger
    val fill = state.color

    Box(
        modifier = modifier
            .requiredSize(burstSize)
            .zIndex(20f)
            .graphicsLayer {
                scaleX = scale.value; scaleY = scale.value
                rotationZ = spin.value; this.alpha = alpha.value
            },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
            val cx = this.size.width / 2f
            val outer = cx * 0.78f
            val burst = starburstPath(cx, cx, outer, outer * 0.62f, 12, seed)
            // pixel particles flying outward
            val rnd = Random(seed * 31)
            val p = particles.value
            repeat(12) { i ->
                val angle = (Math.PI * 2 * i / 12) + rnd.nextDouble(-0.25, 0.25)
                val dist = outer * (0.6f + p * (0.7f + rnd.nextFloat() * 0.5f))
                val px = cx + (cos(angle) * dist).toFloat()
                val py = cx + (sin(angle) * dist).toFloat()
                val s = 5.dp.toPx() * (1f - p * 0.5f)
                drawRect(
                    color = particleColors[i % particleColors.size].copy(alpha = (1f - p).coerceIn(0f, 1f)),
                    topLeft = Offset(px - s / 2, py - s / 2),
                    size = androidx.compose.ui.geometry.Size(s, s)
                )
            }
            // hard green offset shadow, fill, ink outline
            translate(4.dp.toPx(), 4.dp.toPx()) { drawPath(burst, CGreen) }
            drawPath(burst, fill)
            drawPath(burst, ScrapbookDark, style = Stroke(width = 3.dp.toPx()))
        }
        Text(
            text = state.text,
            style = TextStyle(fontFamily = BangersFontFamily, fontSize = 20.sp, letterSpacing = 1.sp, color = ScrapbookDark)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 5. LEVEL UP SPLASH
// ═══════════════════════════════════════════════════════════════════════════════

/** Lets any screen show the splash on demand (e.g. long-press the level badge to preview). */
object LevelUpBus {
    var pending by mutableStateOf<RetroLevel?>(null)
    fun show(level: RetroLevel) { pending = level }
}

/** Watches XP and celebrates when the user's level goes up. Place once at the app root. */
@Composable
fun LevelUpWatcher(achievementsViewModel: AchievementsViewModel) {
    val state by achievementsViewModel.state.collectAsState()
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val loaded = state.badges.isNotEmpty()

    LaunchedEffect(state.xp, loaded, uid) {
        if (!loaded || uid == null) return@LaunchedEffect
        val prefs = context.getSharedPreferences("retrohub_levels", Context.MODE_PRIVATE)
        val key = "lastLevel_$uid"
        val current = getRetroLevel(state.xp)
        val last = prefs.getInt(key, -1)
        if (last != -1 && current.level > last) LevelUpBus.show(current)
        prefs.edit().putInt(key, current.level).apply()
    }

    LevelUpBus.pending?.let { level ->
        LevelUpOverlay(level = level, onDismiss = { LevelUpBus.pending = null })
    }
}

@Composable
fun LevelUpOverlay(level: RetroLevel, onDismiss: () -> Unit) {
    val haptic = rememberComicHaptic()
    val dim = remember { Animatable(0f) }
    val panelScale = remember { Animatable(0f) }
    val stamp = remember { Animatable(3f) }
    val shake = remember { Animatable(0f) }
    val particles = remember { Animatable(0f) }
    val spinT = rememberInfiniteTransition(label = "levelUpSpin")
    val spin by spinT.animateFloat(
        0f, 360f, infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "levelUpSpinA"
    )
    val accent = if (level.level == 1) CGreen else level.color

    LaunchedEffect(level) {
        launch { dim.animateTo(1f, tween(250)) }
        delay(120)
        haptic()
        Chiptune.play(Chiptune.Sfx.POWER_UP)
        RobotBrain.notify(RobotTrigger.HypeMoment)
        launch { panelScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 380f)) }
        launch { particles.animateTo(1f, tween(1600, easing = FastOutSlowInEasing)) }
        delay(380)
        stamp.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 700f))
        haptic()
        Chiptune.play(Chiptune.Sfx.STAMP)
        repeat(6) { i -> shake.animateTo(if (i % 2 == 0) 12f else -12f, tween(40)) }
        shake.animateTo(0f, tween(40))
        delay(3000)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
            .background(Color.Black.copy(alpha = 0.6f * dim.value))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // Radial comic speed lines + confetti
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = dim.value }) {
            val c = center
            val r = size.maxDimension
            rotate(spin, c) {
                for (i in 0 until 36) {
                    val a1 = Math.toRadians(i * 10.0)
                    val a2 = Math.toRadians(i * 10.0 + 4.5)
                    val wedge = Path().apply {
                        moveTo(c.x, c.y)
                        lineTo(c.x + (cos(a1) * r).toFloat(), c.y + (sin(a1) * r).toFloat())
                        lineTo(c.x + (cos(a2) * r).toFloat(), c.y + (sin(a2) * r).toFloat())
                        close()
                    }
                    drawPath(wedge, if (i % 2 == 0) accent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.10f))
                }
            }
            val rnd = Random(level.level * 97)
            val p = particles.value
            repeat(40) { i ->
                val angle = rnd.nextDouble(0.0, Math.PI * 2)
                val dist = r * 0.12f + p * r * (0.25f + rnd.nextFloat() * 0.35f)
                val px = c.x + (cos(angle) * dist).toFloat()
                val py = c.y + (sin(angle) * dist).toFloat() + p * p * 120f
                val s = (4 + rnd.nextInt(6)).dp.toPx()
                drawRect(
                    color = particleColors[i % particleColors.size].copy(alpha = (1f - p * 0.8f).coerceIn(0f, 1f)),
                    topLeft = Offset(px - s / 2, py - s / 2),
                    size = androidx.compose.ui.geometry.Size(s, s)
                )
            }
        }

        // The comic panel
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = panelScale.value; scaleY = panelScale.value
                translationX = shake.value
                rotationZ = -3f
            }
        ) {
            Box(modifier = Modifier.matchParentSize().offset(x = 8.dp, y = 8.dp)
                .clip(RoundedCornerShape(16.dp)).background(accent))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(4.dp, ScrapbookDark, RoundedCornerShape(16.dp))
            ) {
                HalftoneDots(Modifier.matchParentSize(), spacing = 7.dp, dotRadius = 1.4.dp,
                    color = accent.copy(alpha = 0.22f))
                Column(
                    modifier = Modifier.padding(horizontal = 36.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("LEVEL UP!", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 52.sp,
                        letterSpacing = 2.sp, color = ScrapbookDark))
                    Spacer(Modifier.height(4.dp))
                    Text(level.emoji, fontSize = 56.sp)
                    Spacer(Modifier.height(8.dp))
                    // Stamp slams down
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = stamp.value; scaleY = stamp.value
                                alpha = (3f - stamp.value).coerceIn(0f, 1f)
                                rotationZ = -8f
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .background(accent)
                            .border(3.dp, ScrapbookDark, RoundedCornerShape(8.dp))
                            .padding(horizontal = 18.dp, vertical = 6.dp)
                    ) {
                        Text("LV ${level.level}", style = TextStyle(fontFamily = BangersFontFamily,
                            fontSize = 30.sp, letterSpacing = 2.sp, color = Color.White))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(level.title.uppercase(), style = TextStyle(fontFamily = BangersFontFamily,
                        fontSize = 26.sp, letterSpacing = 1.sp, color = ScrapbookDark))
                    Spacer(Modifier.height(6.dp))
                    Text("TAP TO CONTINUE", style = TextStyle(fontFamily = NunitoFontFamily,
                        fontSize = 11.sp, color = ScrapbookDark.copy(alpha = 0.5f)))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 6. ROLLING ODOMETER TEXT — each character rolls up/down when it changes
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun RollingCounterText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        text.forEachIndexed { index, ch ->
            // key from the right so the ones-digit stays the ones-digit when the length changes
            key(text.length - index) {
                AnimatedContent(
                    targetState = ch,
                    transitionSpec = {
                        val up = targetState > initialState
                        (slideInVertically(tween(320)) { h -> if (up) h else -h } + fadeIn(tween(320))) togetherWith
                            (slideOutVertically(tween(320)) { h -> if (up) -h else h } + fadeOut(tween(200))) using
                            SizeTransform(clip = true)
                    },
                    label = "rollingDigit"
                ) { c -> Text(c.toString(), style = style) }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 7. SHARED ELEMENT HELPERS
// Screens provide scopes via CompositionLocals so deep composables can tag
// images without threading scopes through every function signature.
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Tags this element for a shared transition. No-op when no scope is provided. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedCover(key: String): Modifier {
    val shared = LocalSharedScope.current ?: return this
    val anim = LocalAnimScope.current ?: return this
    return with(shared) {
        this@sharedCover.sharedElement(
            state = rememberSharedContentState(key = key),
            animatedVisibilityScope = anim
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 8. COMIC PAGE HEADER — the green Discover-style header, reusable on every page
// ═══════════════════════════════════════════════════════════════════════════════

/** MainActivity registers the drawer opener here so any page header can show a menu button. */
object DrawerController {
    var open: (() -> Unit)? = null
}

@Composable
fun ComicIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    onClick: () -> Unit
) {
    val tapHaptic = rememberTapHaptic()
    var pressed by remember { mutableStateOf(false) }
    val press by animateFloatAsState(if (pressed) 2f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "cibPress")
    Box(modifier = Modifier.size(40.dp)) {
        Box(modifier = Modifier.size(38.dp).offset(x = 2.dp, y = 2.dp)
            .clip(androidx.compose.foundation.shape.CircleShape).background(ScrapbookDark))
        Box(
            modifier = Modifier.size(38.dp)
                .offset(x = press.dp, y = press.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color.White.copy(alpha = 0.7f))
                .border(2.dp, ScrapbookDark, androidx.compose.foundation.shape.CircleShape)
                .clickable { pressed = true; tapHaptic(); onClick() },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(icon, contentDescription, tint = ScrapbookDark, modifier = Modifier.size(19.dp))
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(140); pressed = false } }
}

/** Small dark action chip used in headers (e.g. "▶ POST", "MARK ALL READ"). */
@Composable
fun ComicHeaderChip(text: String, onClick: () -> Unit) {
    val tapHaptic = rememberTapHaptic()
    var pressed by remember { mutableStateOf(false) }
    val press by animateFloatAsState(if (pressed) 2f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "chipPress")
    Box {
        Box(modifier = Modifier.matchParentSize().offset(x = 2.dp, y = 2.dp)
            .clip(RoundedCornerShape(8.dp)).background(ScrapbookDark))
        Box(
            modifier = Modifier
                .offset(x = press.dp, y = press.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.85f))
                .border(2.dp, ScrapbookDark, RoundedCornerShape(8.dp))
                .clickable { pressed = true; tapHaptic(); onClick() }
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(text, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 13.sp,
                letterSpacing = 0.5.sp, color = ScrapbookDark))
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(140); pressed = false } }
}

@Composable
fun ComicPageHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    showMenu: Boolean = true,
    marquee: String? = null,          // scrolling racing-stripe strip under the header
    extra: @Composable ColumnScope.() -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ComicPageHeaderBar(title, subtitle, Modifier, onBack, showMenu, extra, trailing)
        if (marquee != null) ComicMarqueeStrip(marquee)
    }
}

@Composable
private fun ComicPageHeaderBar(
    title: String,
    subtitle: String,
    modifier: Modifier,
    onBack: (() -> Unit)?,
    showMenu: Boolean,
    extra: @Composable ColumnScope.() -> Unit,
    trailing: @Composable RowScope.() -> Unit
) {
    val t = rememberInfiniteTransition(label = "pageHeader")
    val scanX by t.animateFloat(-400f, 400f,
        infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "pageHeaderScan")
    val shimmerX by t.animateFloat(-300f, 600f,
        infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart), label = "pageHeaderShimmer")
    val openDrawer = DrawerController.open
    // Title slams in every time the page opens
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 320f)) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(CGreen, CGreenMint, CGreen)))
            .border(androidx.compose.foundation.BorderStroke(2.dp, ScrapbookBorder))
            .padding(top = 16.dp, bottom = 14.dp, start = 16.dp, end = 16.dp)
    ) {
        HalftoneDots(Modifier.matchParentSize(), spacing = 7.dp, dotRadius = 1.2.dp,
            color = Color.White.copy(alpha = 0.22f))
        // Scan line
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter)
            .background(androidx.compose.ui.graphics.Brush.horizontalGradient(
                colors = listOf(Color.Transparent, ScrapbookDark.copy(alpha = 0.3f), Color.Transparent),
                startX = scanX, endX = scanX + 200f)))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            when {
                onBack != null -> {
                    ComicIconButton(Icons.Filled.ArrowBack, "Back", onBack)
                    Spacer(Modifier.width(12.dp))
                }
                showMenu && openDrawer != null -> {
                    ComicIconButton(Icons.Filled.Menu, "Menu", openDrawer)
                    Spacer(Modifier.width(12.dp))
                }
            }
            Column(modifier = Modifier.weight(1f).konamiCode().graphicsLayer {
                val e = enter.value
                scaleX = 1.3f - 0.3f * e; scaleY = 1.3f - 0.3f * e
                rotationZ = (1f - e) * -5f
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                alpha = e.coerceIn(0f, 1f)
            }) {
                Box {
                    Text(title, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 36.sp,
                        letterSpacing = 2.sp, color = ScrapbookDark), maxLines = 1)
                    Text(title, maxLines = 1, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 36.sp,
                        letterSpacing = 2.sp, brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent),
                            start = Offset(shimmerX - 100f, 0f), end = Offset(shimmerX + 100f, 0f))))
                }
                Text(subtitle, style = TextStyle(fontFamily = NunitoFontFamily,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 12.sp))
                extra()
            }
            trailing()
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 9. ENTRANCE & MOTION MODIFIERS
// ═══════════════════════════════════════════════════════════════════════════════

private val stampedKeys = HashSet<Any>()

/** Card slams down like a rubber stamp — only the first time a given key appears (no replay on scroll). */
@Composable
fun Modifier.stampIn(key: Any, delayMs: Int = 0): Modifier {
    val firstTime = remember(key) { stampedKeys.add(key) }
    if (!firstTime) return this
    val scale = remember(key) { Animatable(1.3f) }
    val alpha = remember(key) { Animatable(0f) }
    val rot = remember(key) { Animatable(if (key.hashCode() % 2 == 0) -4f else 4f) }
    LaunchedEffect(key) {
        delay(delayMs.toLong())
        launch { alpha.animateTo(1f, tween(110)) }
        launch { rot.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 420f)) }
        scale.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 520f))
    }
    return this.graphicsLayer {
        scaleX = scale.value; scaleY = scale.value
        rotationZ = rot.value; this.alpha = alpha.value
    }
}

/** Swings in like a tag hanging from a string (pivot at the top). */
@Composable
fun Modifier.swingIn(key: Any = Unit): Modifier {
    val rot = remember(key) { Animatable(-30f) }
    LaunchedEffect(key) { rot.animateTo(0f, spring(dampingRatio = 0.16f, stiffness = 110f)) }
    return this.graphicsLayer {
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
        rotationZ = rot.value
    }
}

/** Speech-bubble pop from the tail corner. Pass enabled=false for old messages. */
@Composable
fun Modifier.bubblePop(key: Any, fromRight: Boolean, enabled: Boolean = true): Modifier {
    if (!enabled) return this
    val firstTime = remember(key) { stampedKeys.add("bubble_$key") }
    if (!firstTime) return this
    val s = remember(key) { Animatable(0.15f) }
    LaunchedEffect(key) { s.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 520f)) }
    return this.graphicsLayer {
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(if (fromRight) 1f else 0f, 1f)
        scaleX = s.value; scaleY = s.value
        alpha = s.value.coerceIn(0f, 1f)
    }
}

/** Squash-and-stretch bounce when an item becomes selected (bottom nav icons). */
@Composable
fun Modifier.squashOnSelect(selected: Boolean): Modifier {
    val sx = remember { Animatable(1f) }
    val sy = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (!selected) return@LaunchedEffect
        sx.snapTo(1.35f); sy.snapTo(0.65f)
        launch { sx.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = 600f)) }
        sy.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = 600f))
    }
    return this.graphicsLayer {
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
        scaleX = sx.value; scaleY = sy.value
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 10. TYPEWRITER TEXT — types itself out with a blinking block cursor
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun TypewriterText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    charDelayMs: Long = 28L,
    maxLines: Int = Int.MAX_VALUE
) {
    var count by remember(text) { mutableIntStateOf(0) }
    LaunchedEffect(text) {
        delay(200)
        while (count < text.length) { delay(charDelayMs); count++ }
    }
    val blink by rememberGlowRange(0f, 1f)
    Box(modifier = modifier) {
        // invisible full text reserves the final size so layout never jumps
        Text(text, style = style.copy(color = Color.Transparent), maxLines = maxLines)
        val typing = count < text.length
        Text(
            text = text.take(count) + if (typing || blink > 0.5f) "▌" else "",
            style = style, maxLines = maxLines
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 11. TV STATIC — a burst of analog noise, e.g. before opening a stream
// Put TvStaticOverlay() once in a screen; call TvStaticBus.play { action } anywhere.
// ═══════════════════════════════════════════════════════════════════════════════

object TvStaticBus {
    var trigger by mutableIntStateOf(0)
    fun play(action: () -> Unit = {}) {
        trigger++
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ action() }, 380)
    }
}

@Composable
fun TvStaticOverlay(modifier: Modifier = Modifier) {
    val haptic = rememberTapHaptic()
    val alpha = remember { Animatable(0f) }
    var seed by remember { mutableIntStateOf(0) }
    val trigger = TvStaticBus.trigger
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        haptic()
        Chiptune.play(Chiptune.Sfx.NOISE)
        alpha.snapTo(1f)
        repeat(9) { seed++; delay(33) }
        alpha.animateTo(0f, tween(160))
    }
    if (alpha.value <= 0f) return
    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxSize().zIndex(50f).graphicsLayer { this.alpha = alpha.value }) {
        val rnd = Random(seed)
        val cell = 6.dp.toPx()
        val cols = (size.width / cell).toInt() + 1
        val rows = (size.height / cell).toInt() + 1
        drawRect(Color(0xFF111111))
        for (r in 0 until rows) for (c in 0 until cols) {
            val v = rnd.nextFloat()
            if (v > 0.45f) drawRect(
                Color(v, v, v, 1f),
                topLeft = Offset(c * cell, r * cell),
                size = androidx.compose.ui.geometry.Size(cell, cell)
            )
        }
        // rolling bright band
        val bandY = (seed % 9) / 9f * size.height
        drawRect(Color.White.copy(alpha = 0.25f), topLeft = Offset(0f, bandY),
            size = androidx.compose.ui.geometry.Size(size.width, 18.dp.toPx()))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 12. CRT POWER-ON — content expands from a thin line with a white flash
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun CrtPowerOn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val sx = remember { Animatable(0.3f) }
    val sy = remember { Animatable(0.006f) }
    val flash = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        sx.animateTo(1f, tween(150))
        sy.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
        flash.animateTo(0f, tween(320))
    }
    Box(modifier = modifier.graphicsLayer { scaleX = sx.value; scaleY = sy.value }) {
        content()
        if (flash.value > 0f) Box(Modifier.matchParentSize().background(Color.White.copy(alpha = flash.value * 0.85f)))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 13. BLINKING ARCADE TEXT — "INSERT COIN" style
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun ArcadeBlinkText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "arcadeBlink")
    val on by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart), label = "arcadeBlinkA")
    Text(text, style = style, modifier = modifier.graphicsLayer { alpha = if (on < 0.6f) 1f else 0f })
}


// ═══════════════════════════════════════════════════════════════════════════════
// 14. COMIC MARQUEE STRIP — green racing-stripe band with endlessly scrolling text
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun ComicMarqueeStrip(text: String, modifier: Modifier = Modifier) {
    var chunkWidthPx by remember { mutableStateOf(0f) }
    val scrollOffset = remember { Animatable(0f) }
    LaunchedEffect(chunkWidthPx) {
        if (chunkWidthPx <= 0f) return@LaunchedEffect
        while (true) {
            scrollOffset.snapTo(0f)
            scrollOffset.animateTo(
                targetValue = -chunkWidthPx,
                animationSpec = tween(
                    durationMillis = (chunkWidthPx / 120f * 1000f).toInt().coerceIn(4000, 16000),
                    easing = LinearEasing
                )
            )
        }
    }
    val chunk = "  $text  •  "
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(CGreen)
            .border(androidx.compose.foundation.BorderStroke(1.5.dp, ScrapbookDark))
            .clipToBounds()
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
            var x = -size.height
            while (x < size.width + size.height) {
                drawLine(
                    color = ScrapbookDark.copy(alpha = 0.16f),
                    start = Offset(x, size.height),
                    end = Offset(x + size.height, 0f),
                    strokeWidth = 16f
                )
                x += 32f
            }
        }
        Row(
            modifier = Modifier
                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                .fillMaxHeight()
                .graphicsLayer { translationX = scrollOffset.value },
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(8) { index ->
                Text(
                    text = chunk,
                    style = TextStyle(fontFamily = BangersFontFamily, color = ScrapbookDark,
                        fontSize = 12.sp, letterSpacing = 1.sp),
                    maxLines = 1, softWrap = false,
                    modifier = if (index == 0) Modifier.onSizeChanged { sz ->
                        if (chunkWidthPx == 0f) chunkWidthPx = sz.width.toFloat()
                    } else Modifier
                )
            }
        }
    }
}


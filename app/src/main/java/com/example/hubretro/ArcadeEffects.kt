package com.example.hubretro

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.hubretro.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Calendar
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// ═══════════════════════════════════════════════════════════════════════════════
// 1. CHIPTUNE SFX — real 8-bit square-wave sounds synthesized in code (no assets)
// ═══════════════════════════════════════════════════════════════════════════════

object Chiptune {
    enum class Sfx { COIN, POWER_UP, POP, FANFARE, NOISE, STAMP, SECRET, BLIP }

    private const val RATE = 22050
    private const val PREFS = "retrohub_sfx"
    private val cache = HashMap<Sfx, ShortArray>()
    private val pool = Executors.newCachedThreadPool()

    @Volatile private var muted = false
    /** Observable copy for UI (speaker icon). */
    var isMuted by mutableStateOf(false)
        private set

    fun init(context: Context) {
        muted = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("muted", false)
        isMuted = muted
    }

    fun toggleMute(context: Context) {
        muted = !muted
        isMuted = muted
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("muted", muted).apply()
        if (!muted) play(Sfx.COIN)
    }

    fun play(sfx: Sfx) {
        if (muted) return
        pool.execute {
            try {
                val pcm = synchronized(cache) { cache.getOrPut(sfx) { synth(sfx) } }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep(pcm.size * 1000L / RATE + 60)
                track.release()
            } catch (_: Exception) { }
        }
    }

    private class Note(val f0: Float, val f1: Float, val ms: Int, val noise: Boolean = false, val vol: Float = 0.22f)

    private fun seq(vararg notes: Note): ShortArray {
        val total = notes.sumOf { it.ms * RATE / 1000 }
        val out = ShortArray(total)
        var idx = 0
        var phase = 0.0
        val rnd = Random(7)
        for (note in notes) {
            val len = note.ms * RATE / 1000
            for (i in 0 until len) {
                val t = i.toFloat() / len
                val f = note.f0 + (note.f1 - note.f0) * t
                phase += f / RATE
                if (phase >= 1.0) phase -= floor(phase)
                val raw = when {
                    note.noise -> rnd.nextFloat() * 2f - 1f
                    phase < 0.5 -> 1f
                    else -> -1f
                }
                val env = (1f - t).pow(0.6f) * min(1f, i / 40f)
                out[idx++] = (raw * env * note.vol * Short.MAX_VALUE).toInt().toShort()
            }
        }
        return out
    }

    private fun tones(vararg hz: Float, ms: Int) = hz.map { Note(it, it, ms) }.toTypedArray()

    private fun synth(s: Sfx): ShortArray = when (s) {
        Sfx.COIN     -> seq(Note(988f, 988f, 70), Note(1319f, 1319f, 260))
        Sfx.POWER_UP -> seq(*tones(523f, 659f, 784f, 1047f, 1319f, 1568f, ms = 60), Note(2093f, 2093f, 240))
        Sfx.POP      -> seq(Note(350f, 1200f, 90))
        Sfx.FANFARE  -> seq(Note(784f, 784f, 100), Note(784f, 784f, 100), Note(784f, 784f, 100), Note(1047f, 1047f, 380))
        Sfx.NOISE    -> seq(Note(0f, 0f, 280, noise = true, vol = 0.14f))
        Sfx.STAMP    -> seq(Note(260f, 55f, 140, vol = 0.3f))
        Sfx.SECRET   -> seq(*tones(1047f, 1319f, 1568f, 2093f, 1568f, 2093f, ms = 70), Note(2637f, 2637f, 320))
        Sfx.BLIP     -> seq(Note(880f, 880f, 45, vol = 0.15f))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 2. COMIC-PANEL SCREEN TRANSITION — new screen slashes in on a diagonal ink cut
// Use inside AnimatedContent: Modifier.comicPanelTransition(this@AnimatedContent)
// with transitionSpec EnterTransition.None / ExitTransition.None.
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun Modifier.comicPanelTransition(scope: AnimatedVisibilityScope): Modifier {
    val progress = scope.transition.animateFloat(
        transitionSpec = { tween(520, easing = FastOutSlowInEasing) },
        label = "comicPanelReveal"
    ) { state -> if (state == EnterExitState.Visible) 1f else 0f }
    val entering = scope.transition.targetState == EnterExitState.Visible

    return if (entering) {
        this.drawWithContent {
            val p = progress.value
            if (p >= 0.999f) { drawContent(); return@drawWithContent }
            val slant = size.height * 0.35f
            val edge = p * (size.width + slant)
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(edge, 0f)
                lineTo(edge - slant, size.height)
                lineTo(0f, size.height)
                close()
            }
            clipPath(path) { this@drawWithContent.drawContent() }
            // ink cut: green offset shadow + thick black line
            val sh = 7.dp.toPx()
            drawLine(CGreen, Offset(edge + sh, 0f), Offset(edge - slant + sh, size.height), strokeWidth = 9.dp.toPx())
            drawLine(ScrapbookDark, Offset(edge, 0f), Offset(edge - slant, size.height), strokeWidth = 5.dp.toPx())
        }
    } else {
        this.graphicsLayer {
            val p = progress.value
            val s = 0.93f + 0.07f * p
            scaleX = s; scaleY = s
            alpha = 0.35f + 0.65f * p
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 3. GAME OVER (errors) & NO SAVE DATA (empty states)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun GameOverState(message: String, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    var retryKey by remember { mutableIntStateOf(0) }
    var count by remember { mutableIntStateOf(9) }
    var over by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }
    val haptic = rememberComicHaptic()

    LaunchedEffect(Unit) {
        haptic()
        repeat(6) { i -> shake.animateTo(if (i % 2 == 0) 10f else -10f, tween(45)) }
        shake.animateTo(0f, tween(45))
    }
    LaunchedEffect(retryKey) {
        count = 9; over = false
        if (onRetry == null) return@LaunchedEffect
        while (count > 0) {
            delay(1000)
            count--
            Chiptune.play(Chiptune.Sfx.BLIP)
        }
        over = true
    }

    Box(modifier = modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.graphicsLayer { translationX = shake.value; rotationZ = -1.5f }) {
            Box(modifier = Modifier.matchParentSize().offset(x = 6.dp, y = 6.dp)
                .clip(RoundedCornerShape(16.dp)).background(CAcRed))
            // Intentionally dark: it's an arcade CRT screen
            Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(ScrapbookDark)
                .border(3.dp, ScrapbookDark, RoundedCornerShape(16.dp))) {
                HalftoneDots(Modifier.matchParentSize(), spacing = 4.dp, dotRadius = 0.8.dp,
                    color = Color.White.copy(alpha = 0.06f))
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("GAME OVER", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 44.sp,
                        letterSpacing = 3.sp, color = CAcRed))
                    Spacer(Modifier.height(4.dp))
                    Text(message, style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center))
                    if (onRetry != null) {
                        Spacer(Modifier.height(14.dp))
                        if (!over) {
                            Text("CONTINUE?", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 22.sp,
                                letterSpacing = 2.sp, color = CAcYellowL))
                            RollingCounterText("$count", TextStyle(fontFamily = BangersFontFamily,
                                fontSize = 56.sp, color = Color.White))
                        } else {
                            ArcadeBlinkText("PRESS START", TextStyle(fontFamily = BangersFontFamily,
                                fontSize = 22.sp, letterSpacing = 2.sp, color = Color.White))
                        }
                        Spacer(Modifier.height(12.dp))
                        Box {
                            Box(modifier = Modifier.matchParentSize().offset(x = 3.dp, y = 3.dp)
                                .clip(RoundedCornerShape(10.dp)).background(CGreenDeep))
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(CGreen)
                                    .border(2.dp, Color.White, RoundedCornerShape(10.dp))
                                    .clickable {
                                        Chiptune.play(Chiptune.Sfx.COIN)
                                        retryKey++
                                        onRetry()
                                    }
                                    .padding(horizontal = 22.dp, vertical = 10.dp)
                            ) {
                                Text("▶ INSERT COIN", style = TextStyle(fontFamily = BangersFontFamily,
                                    fontSize = 16.sp, letterSpacing = 1.sp, color = ScrapbookDark))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoSaveDataState(
    subtitle: String,
    modifier: Modifier = Modifier,
    title: String = "NO SAVE DATA",
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    val float by rememberGlowRange(-4f, 4f)
    Column(modifier = modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // PS1-style memory card
        Box(modifier = Modifier.graphicsLayer { translationY = float; rotationZ = -4f }) {
            Box(modifier = Modifier.size(width = 64.dp, height = 84.dp).offset(x = 4.dp, y = 4.dp)
                .clip(RoundedCornerShape(6.dp)).background(CGreen))
            Box(modifier = Modifier.size(width = 64.dp, height = 84.dp).clip(RoundedCornerShape(6.dp))
                .background(Color.White).border(2.5.dp, ScrapbookDark, RoundedCornerShape(6.dp))) {
                Column(Modifier.fillMaxSize().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(3.dp)).background(CGreen)
                        .border(1.5.dp, ScrapbookDark, RoundedCornerShape(3.dp)), contentAlignment = Alignment.Center) {
                        Text("MEMORY\nCARD", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 8.sp,
                            lineHeight = 9.sp, color = ScrapbookDark, textAlign = TextAlign.Center))
                    }
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(5) { Box(Modifier.size(width = 5.dp, height = 12.dp).background(ScrapbookDark)) }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 26.sp,
            letterSpacing = 2.sp, color = ScrapbookDark))
        ArcadeBlinkText("SLOT 1 — EMPTY", TextStyle(fontFamily = BangersFontFamily, fontSize = 13.sp,
            letterSpacing = 2.sp, color = CGreenDeep))
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp,
            color = ScrapbookTextMuted, textAlign = TextAlign.Center, lineHeight = 18.sp))
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            RetroGlassButton(text = actionText, onClick = onAction, modifier = Modifier.fillMaxWidth(), burstText = "NEW GAME!")
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 4. ACHIEVEMENT UNLOCKED TOASTS
// ═══════════════════════════════════════════════════════════════════════════════

object AchievementToastBus {
    val queue = mutableStateListOf<Badge>()
}

/** Place once at the app root. Detects newly earned badges and queues toasts. */
@Composable
fun AchievementWatcher(achievementsViewModel: AchievementsViewModel) {
    val state by achievementsViewModel.state.collectAsState()
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(state.badges, uid) {
        if (state.badges.isEmpty() || uid == null) return@LaunchedEffect
        val prefs = context.getSharedPreferences("retrohub_badges", Context.MODE_PRIVATE)
        val key = "earned_$uid"
        val earnedNow = state.badges.filter { it.isEarned }.map { it.id }.toSet()
        val saved = prefs.getStringSet(key, null)
        if (saved != null) {
            state.badges.filter { it.isEarned && it.id !in saved }.forEach { AchievementToastBus.queue.add(it) }
        }
        prefs.edit().putStringSet(key, earnedNow).apply()
    }

    val current = AchievementToastBus.queue.firstOrNull()
    if (current != null) {
        key(current.id) {
            AchievementToast(current) { AchievementToastBus.queue.remove(current) }
        }
    }
}

@Composable
fun AchievementToast(badge: Badge, onDone: () -> Unit) {
    val haptic = rememberComicHaptic()
    val slide = remember { Animatable(-220f) }
    val spinT = rememberInfiniteTransition(label = "trophySpin")
    val spin by spinT.animateFloat(0f, 360f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "trophySpinA")

    LaunchedEffect(badge.id) {
        Chiptune.play(Chiptune.Sfx.FANFARE)
        haptic()
        RobotBrain.notify(RobotTrigger.HypeMoment)
        slide.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 380f))
        delay(2800)
        slide.animateTo(-260f, tween(260))
        onDone()
    }

    Box(
        modifier = Modifier.fillMaxWidth().zIndex(95f)
            .statusBarsPadding()
            .padding(top = 8.dp, start = 16.dp, end = 16.dp)
            .graphicsLayer { translationY = slide.value * density }
    ) {
        Box(modifier = Modifier.matchParentSize().offset(x = 5.dp, y = 5.dp)
            .clip(RoundedCornerShape(14.dp)).background(CAcYellow))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .border(3.dp, ScrapbookDark, RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp)
                    .graphicsLayer { rotationY = spin; cameraDistance = 10f * density }
                    .clip(CircleShape).background(badge.color.copy(alpha = 0.9f))
                    .border(2.5.dp, ScrapbookDark, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text(badge.emoji, fontSize = 22.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("🏆 ACHIEVEMENT UNLOCKED", style = TextStyle(fontFamily = BangersFontFamily,
                    fontSize = 12.sp, letterSpacing = 1.5.sp, color = CGreenDeep))
                Text(badge.name, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 20.sp,
                    color = ScrapbookDark), maxLines = 1)
                Text(badge.description, style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 11.sp,
                    color = ScrapbookTextMuted), maxLines = 1)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 5. DEVICE TILT + HOLOGRAPHIC BADGE CARDS
// ═══════════════════════════════════════════════════════════════════════════════

/** Smoothed phone tilt in -1..1 on both axes. Share one per screen section. */
@Composable
fun rememberDeviceTilt(): State<Offset> {
    val context = LocalContext.current
    val tilt = remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var fx = 0f
        var fy = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                fx = fx * 0.85f + (-e.values[0] / 9.81f) * 0.15f
                fy = fy * 0.85f + ((e.values[1] / 9.81f) - 0.6f) * 0.15f
                tilt.value = Offset(fx.coerceIn(-1f, 1f), fy.coerceIn(-1f, 1f))
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }
    return tilt
}

private val holoColors = listOf(
    Color.Transparent,
    Color(0x66FFFFFF), Color(0x6695D5B2), Color(0x88FFFFFF), Color(0x6652B788),
    Color.Transparent
)

@Composable
fun HoloBadgeCard(badge: Badge, tilt: State<Offset>) {
    var flipped by remember { mutableStateOf(false) }
    val flip by animateFloatAsState(if (flipped) 180f else 0f,
        spring(dampingRatio = 0.6f, stiffness = 300f), label = "holoFlip")
    val sweepT = rememberInfiniteTransition(label = "holoSweep")
    val sweep by sweepT.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "holoSweepA")
    val tap = rememberTapHaptic()
    val earned = badge.isEarned
    val showBack = flip > 90f
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier.width(78.dp).height(108.dp)
            .graphicsLayer {
                val t = tilt.value
                rotationY = flip + (if (earned) t.x * 14f else 0f)
                rotationX = if (earned) -t.y * 10f else 0f
                cameraDistance = 12f * density
            }
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                flipped = !flipped
                tap()
                Chiptune.play(Chiptune.Sfx.POP)
            }
    ) {
        Box(modifier = Modifier.matchParentSize().offset(x = 3.dp, y = 3.dp).clip(shape)
            .background(if (earned) badge.color else ScrapbookDark.copy(alpha = 0.2f)))
        Box(
            modifier = Modifier.matchParentSize()
                .graphicsLayer { if (showBack) rotationY = 180f }
                .clip(shape).background(Color.White)
                .border(2.5.dp, if (earned) ScrapbookDark else ScrapbookDark.copy(alpha = 0.35f), shape)
        ) {
            if (!showBack) {
                Column(Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f)
                            .background(if (earned) badge.color.copy(alpha = 0.85f) else Color(0xFFE2E2E2)),
                        contentAlignment = Alignment.Center
                    ) {
                        HalftoneDots(Modifier.matchParentSize(), spacing = 5.dp, dotRadius = 1.1.dp,
                            color = Color.White.copy(alpha = 0.3f))
                        Text(if (earned) badge.emoji else "🔒", fontSize = 30.sp,
                            color = if (earned) Color.Unspecified else ScrapbookDark.copy(alpha = 0.35f))
                    }
                    Box(Modifier.fillMaxWidth().height(2.5.dp).background(ScrapbookDark))
                    Text(
                        badge.name.uppercase(),
                        style = TextStyle(fontFamily = BangersFontFamily, fontSize = 10.sp, lineHeight = 11.sp,
                            color = if (earned) ScrapbookDark else ScrapbookTextMuted, textAlign = TextAlign.Center),
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 5.dp)
                    )
                }
                if (earned) {
                    // Holographic foil sweep that also follows the phone tilt
                    Box(Modifier.matchParentSize().drawBehind {
                        val shift = ((sweep + tilt.value.x * 0.4f) % 1f) * size.width * 3f
                        drawRect(Brush.linearGradient(holoColors,
                            start = Offset(shift - size.width * 1.5f, 0f),
                            end = Offset(shift, size.height)))
                    })
                }
            } else {
                Column(
                    Modifier.fillMaxSize().padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(if (earned) "★ EARNED" else "LOCKED", style = TextStyle(fontFamily = BangersFontFamily,
                        fontSize = 11.sp, color = if (earned) CGreenDeep else ScrapbookTextMuted))
                    Spacer(Modifier.height(4.dp))
                    Text(badge.description, style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 9.sp,
                        lineHeight = 11.sp, color = ScrapbookDark, textAlign = TextAlign.Center))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 6. INK SPLAT ON EVERY TAP (app-wide, never consumes touches)
// ═══════════════════════════════════════════════════════════════════════════════

private class InkSplat(val pos: Offset, val seed: Int, val born: Long)
private const val SPLAT_MS = 420L

@Composable
fun Modifier.inkSplatTaps(): Modifier {
    val splats = remember { mutableStateListOf<InkSplat>() }
    var now by remember { mutableLongStateOf(0L) }
    val active = splats.isNotEmpty()
    LaunchedEffect(active) {
        while (splats.isNotEmpty()) {
            withFrameMillis {
                now = System.currentTimeMillis()
                splats.removeAll { now - it.born > SPLAT_MS }
            }
        }
    }
    return this
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                splats.add(InkSplat(down.position, Random.nextInt(), System.currentTimeMillis()))
            }
        }
        .drawWithContent {
            drawContent()
            val t0 = now
            for (s in splats) {
                val age = ((t0 - s.born).toFloat() / SPLAT_MS).coerceIn(0f, 1f)
                val a = 1f - age
                val r = 9.dp.toPx() * (0.6f + age * 0.9f)
                drawCircle(ScrapbookDark.copy(alpha = 0.22f * a), radius = r, center = s.pos)
                drawCircle(CGreen.copy(alpha = 0.5f * a), radius = r * 1.5f, center = s.pos,
                    style = Stroke(width = 2.dp.toPx()))
                val rnd = Random(s.seed)
                repeat(5) {
                    val ang = rnd.nextDouble(0.0, Math.PI * 2)
                    val dist = r * (1.2f + age * 1.4f) * (0.8f + rnd.nextFloat() * 0.5f)
                    drawCircle(
                        ScrapbookDark.copy(alpha = 0.3f * a),
                        radius = (1.5f + rnd.nextFloat() * 2f).dp.toPx(),
                        center = Offset(s.pos.x + (kotlin.math.cos(ang) * dist).toFloat(), s.pos.y + (sin(ang) * dist).toFloat())
                    )
                }
            }
        }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 7. PIXEL DUST (seasonal) + PARTY MODE (cheat-code confetti)
// ═══════════════════════════════════════════════════════════════════════════════

object PartyMode {
    var until by mutableLongStateOf(0L)
    fun start(ms: Long = 12_000L) { until = System.currentTimeMillis() + ms }
}

private val partyColors = listOf(CGreen, CGreenMint, CGreenDeep, Color.White, ScrapbookDark)

/** Very subtle drifting pixels over the app. Snow in Dec/Jan, spooky pixels in Oct, confetti in party mode. */
@Composable
fun PixelDustLayer(modifier: Modifier = Modifier) {
    val month = remember { Calendar.getInstance().get(Calendar.MONTH) }
    var time by remember { mutableLongStateOf(0L) }
    // Only animates during cheat-code party mode, so nothing is ever drawn over the
    // app's content during normal use (keeps text readable, saves battery).
    val partyUntil = PartyMode.until
    var partyOn by remember { mutableStateOf(false) }
    LaunchedEffect(partyUntil) {
        partyOn = partyUntil > System.currentTimeMillis()
        while (isActive && System.currentTimeMillis() < partyUntil) { withFrameMillis { time = it } }
        partyOn = false
    }
    if (!partyOn) return

    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxSize()) {
        val t = time / 1000f
        val party = PartyMode.until > System.currentTimeMillis()
        val rnd = Random(42)
        val count = if (party) 80 else 18
        repeat(count) { i ->
            val baseX = rnd.nextFloat()
            val speed = 0.015f + rnd.nextFloat() * 0.04f
            val phase = rnd.nextFloat()
            val px = (3 + rnd.nextInt(4)).dp.toPx()
            val c = rnd.nextInt(partyColors.size)
            when {
                party -> {
                    val y = ((phase + t * speed * 5f) % 1f) * size.height
                    val x = baseX * size.width + sin(t * 3f + i) * 18.dp.toPx()
                    rotate(t * 200f + i * 23f, Offset(x, y)) {
                        drawRect(partyColors[c], topLeft = Offset(x, y), size = Size(px, px * 1.7f))
                    }
                }
                month == 11 || month == 0 -> { // snow
                    val y = ((phase + t * speed) % 1f) * size.height
                    val x = baseX * size.width + sin(t + i) * 12.dp.toPx()
                    drawCircle(Color(0xFF9CC9E8).copy(alpha = 0.45f), radius = px * 0.6f, center = Offset(x, y))
                }
                else -> { // rising pixel dust
                    val y = (1f - (phase + t * speed) % 1f) * size.height
                    val x = baseX * size.width + sin(t * 0.7f + i) * 10.dp.toPx()
                    val col = if (month == 9) (if (i % 2 == 0) CAcYellow else CAcPurple) else (if (i % 3 == 0) CAcYellowL else CGreen)
                    drawRect(col.copy(alpha = 0.16f), topLeft = Offset(x, y), size = Size(px, px))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 8. PS1 "NOW LOADING"
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun NowLoadingIndicator(modifier: Modifier = Modifier, label: String = "NOW LOADING") {
    val t = rememberInfiniteTransition(label = "nowLoading")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "nowLoadingSpin")
    val dots by t.animateFloat(0f, 4f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "nowLoadingDots")
    Column(modifier = modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(58.dp)
                .graphicsLayer { rotationY = spin; rotationZ = 45f; cameraDistance = 10f * density }
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(CGreenMint, CGreen, CGreenDeep)))
                .border(3.dp, ScrapbookDark, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("RH", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 22.sp, color = ScrapbookDark),
                modifier = Modifier.graphicsLayer { rotationZ = -45f })
        }
        Spacer(Modifier.height(18.dp))
        Text(label + ".".repeat(dots.toInt().coerceIn(0, 3)), style = TextStyle(fontFamily = BangersFontFamily,
            fontSize = 18.sp, letterSpacing = 3.sp, color = ScrapbookDark))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 9. KONAMI CODE  ↑↑↓↓←→←→  (swipe on any page title)
// ═══════════════════════════════════════════════════════════════════════════════

object KonamiCode {
    private val sequence = listOf('U', 'U', 'D', 'D', 'L', 'R', 'L', 'R')
    private var progress = 0

    fun input(c: Char): Boolean {
        if (sequence[progress] == c) {
            progress++
            if (progress == sequence.size) { progress = 0; return true }
        } else {
            progress = if (c == sequence[0]) 1 else 0
        }
        return false
    }

    fun activateCheat() {
        PartyMode.start()
        Chiptune.play(Chiptune.Sfx.SECRET)
        RobotBrain.notify(RobotTrigger.HypeMoment)
        AchievementToastBus.queue.add(
            Badge(
                id = "konami_${System.currentTimeMillis()}",
                name = "CHEAT CODE ACTIVATED",
                description = "+30 lives · party mode on",
                emoji = "🎮",
                color = CAcPurple,
                isEarned = true
            )
        )
    }
}

@Composable
fun Modifier.konamiCode(): Modifier {
    val tap = rememberTapHaptic()
    return this.pointerInput(Unit) {
        var total = Offset.Zero
        detectDragGestures(
            onDragStart = { total = Offset.Zero },
            onDragEnd = {
                if (total.getDistance() > 30.dp.toPx()) {
                    val c = if (abs(total.x) > abs(total.y)) (if (total.x > 0) 'R' else 'L')
                            else (if (total.y > 0) 'D' else 'U')
                    tap()
                    if (KonamiCode.input(c)) KonamiCode.activateCheat()
                }
            }
        ) { change, drag ->
            change.consume()
            total += drag
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 10. SHAKE FOR A SURPRISE GAME
// ═══════════════════════════════════════════════════════════════════════════════

object SurpriseBus {
    var pending by mutableStateOf(false)
}

@Composable
fun ShakeDetector(onShake: () -> Unit) {
    val context = LocalContext.current
    val currentOnShake by rememberUpdatedState(onShake)
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var last = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val g = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2]) / 9.81f
                val now = System.currentTimeMillis()
                if (g > 2.7f && now - last > 2000) {
                    last = now
                    currentOnShake()
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }
}

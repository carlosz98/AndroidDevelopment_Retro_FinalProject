package com.example.hubretro

import androidx.compose.animation.core.*
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.hubretro.ui.theme.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

// ═══════════════════════════════════════════════════════════════════════════════
// Login / Sign-up screen effects
// ═══════════════════════════════════════════════════════════════════════════════

// 8x6 pixel "space invader" sprite (1 = filled)
private val invaderSprite = listOf(
    "00100100",
    "00111100",
    "01011010",
    "11111111",
    "10111101",
    "00100100"
)

/** Faint pixel invaders slowly drifting up behind the auth forms. */
@Composable
fun FloatingInvadersBackground(modifier: Modifier = Modifier) {
    var time by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { while (isActive) { withFrameMillis { time = it } } }
    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxSize()) {
        val t = time / 1000f
        val rnd = Random(11)
        repeat(9) { i ->
            val px = (3 + rnd.nextInt(3)).dp.toPx()
            val speed = 0.02f + rnd.nextFloat() * 0.03f
            val phase = rnd.nextFloat()
            val baseX = rnd.nextFloat()
            val y = (1f - (phase + t * speed) % 1f) * (size.height + 80f) - 40f
            val x = baseX * size.width + sin(t * 0.8f + i) * 14.dp.toPx()
            val color = (if (i % 3 == 0) CGreenDeep else CGreen).copy(alpha = 0.13f)
            invaderSprite.forEachIndexed { row, line ->
                line.forEachIndexed { col, c ->
                    if (c == '1') drawRect(color, topLeft = Offset(x + col * px, y + row * px), size = Size(px, px))
                }
            }
        }
    }
}

/** Coin that drops into a button (arcade "insert coin"). Call fire() on tap. */
class CoinDropState {
    internal var trigger by mutableIntStateOf(0)
    fun fire() { trigger++ }
}

@Composable
fun rememberCoinDrop(): CoinDropState = remember { CoinDropState() }

@Composable
fun CoinDrop(state: CoinDropState, modifier: Modifier = Modifier, size: Dp = 26.dp) {
    val y = remember { Animatable(0f) }
    val spin = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    LaunchedEffect(state.trigger) {
        if (state.trigger == 0) return@LaunchedEffect
        Chiptune.play(Chiptune.Sfx.COIN)
        y.snapTo(-70f); alpha.snapTo(1f); scale.snapTo(1f); spin.snapTo(0f)
        launch { spin.animateTo(720f, tween(420, easing = LinearEasing)) }
        y.animateTo(0f, tween(380, easing = FastOutLinearInEasing))
        launch { scale.animateTo(0.2f, tween(160)) }
        alpha.animateTo(0f, tween(160))
    }
    if (alpha.value <= 0f) return
    Box(
        modifier = modifier
            .zIndex(10f)
            .graphicsLayer {
                translationY = y.value * density
                rotationY = spin.value
                scaleX = scale.value; scaleY = scale.value
                this.alpha = alpha.value
                cameraDistance = 10f * density
            }
            .size(size)
            .clip(CircleShape)
            .background(CAcYellowL)
            .border(2.5.dp, ScrapbookDark, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("★", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 12.sp, color = ScrapbookDark))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// AUTH GATE — while signed out (or mid sign-up) the rest of the app is locked.
// Full-screen, no bottom nav / drawer / top bar. Login ⇄ Create Account slide
// like comic panels; after the account exists, step 2 is profile setup.
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun AuthGate(
    authViewModel: AuthViewModel,
    showCreateAccount: Boolean,
    onShowCreateAccount: (Boolean) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(ComicGlassBg)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            androidx.compose.animation.AnimatedContent(
                targetState = showCreateAccount,
                transitionSpec = {
                    androidx.compose.animation.fadeIn(tween(250)) togetherWith
                        androidx.compose.animation.fadeOut(tween(150))
                },
                label = "authHeader"
            ) { creating ->
                Column {
                    ComicPageHeader(
                        title = if (creating) "ACCOUNT CREATION" else "WELCOME BACK",
                        subtitle = if (creating) "Step 1 of 2 · Create your player" else "Insert coin to continue",
                        showMenu = false,
                        marquee = if (creating) "🕹️ NEW PLAYER  •  PICK A USERNAME  •  SECURE PASSWORD  •  JOIN THE HUB"
                                  else "★ RETROHUB  •  PRESS START  •  SIGN IN  •  PLAYER 1 READY"
                    )
                    if (creating) SignupStepper(step = 1)
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                androidx.compose.animation.AnimatedContent(
                    targetState = showCreateAccount,
                    transitionSpec = {
                        val forward = targetState   // going to Create Account slides in from the right
                        (androidx.compose.animation.slideInHorizontally(spring(dampingRatio = 0.8f, stiffness = 300f)) { w -> if (forward) w else -w } +
                            androidx.compose.animation.fadeIn(tween(200))) togetherWith
                            (androidx.compose.animation.slideOutHorizontally(tween(260)) { w -> if (forward) -w / 3 else w / 3 } +
                                androidx.compose.animation.fadeOut(tween(200)))
                    },
                    label = "authPanels"
                ) { creating ->
                    if (creating) {
                        CreateAccountScreen(
                            authViewModel = authViewModel,
                            onAccountCreated = { onShowCreateAccount(false) },
                            onNavigateToLogin = { onShowCreateAccount(false) }
                        )
                    } else {
                        LoginScreen(
                            authViewModel = authViewModel,
                            onLoginSuccess = { },
                            onNavigateToCreateAccount = { onShowCreateAccount(true) }
                        )
                    }
                }
            }
        }
    }
}

/** Step 2 of account creation: profile setup, still locked full-screen. */
@Composable
fun OnboardingGate(authViewModel: AuthViewModel) {
    Box(modifier = Modifier.fillMaxSize().background(ComicGlassBg)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            // Kept slim so the setup steps (search results etc.) get the space
            ComicPageHeader(
                title = "ACCOUNT CREATION",
                subtitle = "Step 2 of 2 · Set up your profile",
                showMenu = false
            )
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                ProfileSetupScreen(authViewModel = authViewModel, onSetupComplete = { }, compact = true)
            }
        }
    }
}

/** "1 ACCOUNT ─── 2 PROFILE" progress with an animated connecting line. */
@Composable
fun SignupStepper(step: Int) {
    val fill by animateFloatAsState(if (step >= 2) 1f else 0f, tween(700, easing = FastOutSlowInEasing), label = "stepFill")
    val pulse by rememberGlowRange(1f, 1.08f)
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepBubble(number = 1, label = "ACCOUNT", done = step > 1, active = step == 1, pulse = pulse)
        Box(
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp).height(6.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                .background(ScrapbookDark.copy(alpha = 0.1f))
        ) {
            Box(Modifier.fillMaxWidth(fill).fillMaxHeight().background(CGreen))
        }
        StepBubble(number = 2, label = "PROFILE", done = false, active = step == 2, pulse = pulse)
    }
}

@Composable
private fun StepBubble(number: Int, label: String, done: Boolean, active: Boolean, pulse: Float) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier.size(28.dp)
                .graphicsLayer { val s = if (active) pulse else 1f; scaleX = s; scaleY = s }
                .clip(CircleShape)
                .background(if (done || active) CGreen else Color.White)
                .border(2.dp, ScrapbookDark, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(if (done) "✓" else "$number", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 14.sp, color = ScrapbookDark))
        }
        Text(label, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 13.sp, letterSpacing = 1.sp,
            color = if (done || active) ScrapbookDark else ScrapbookTextMuted))
    }
}

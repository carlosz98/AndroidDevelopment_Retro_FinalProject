package com.example.hubretro

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hubretro.ui.theme.*

@Composable
fun CreateAccountScreen(
    authViewModel: AuthViewModel = viewModel(),
    onAccountCreated: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()

    var username         by remember { mutableStateOf("") }
    var email            by remember { mutableStateOf("") }
    var password         by remember { mutableStateOf("") }
    var confirmPassword  by remember { mutableStateOf("") }
    var passwordMismatch by remember { mutableStateOf(false) }

    // ── Live validation ──────────────────────────────────────────────────
    val usernameError = SignupRules.usernameError(username)
    val emailError = SignupRules.emailError(email)
    val pwCheck = SignupRules.checkPassword(password)
    val passwordsMatch = confirmPassword.isNotEmpty() && confirmPassword == password
    var usernameAvail by remember { mutableStateOf(Availability.UNKNOWN) }
    var emailAvail by remember { mutableStateOf(Availability.UNKNOWN) }

    LaunchedEffect(username) {
        usernameAvail = Availability.UNKNOWN
        if (SignupRules.usernameError(username) == null) {
            kotlinx.coroutines.delay(600)            // wait until they stop typing
            usernameAvail = Availability.CHECKING
            usernameAvail = SignupAvailability.username(username)
        }
    }
    LaunchedEffect(email) {
        emailAvail = Availability.UNKNOWN
        if (SignupRules.emailError(email) == null) {
            kotlinx.coroutines.delay(600)
            emailAvail = Availability.CHECKING
            emailAvail = SignupAvailability.email(email)
        }
    }

    val formValid = usernameError == null && usernameAvail != Availability.TAKEN &&
        emailError == null && emailAvail != Availability.TAKEN &&
        pwCheck.allPassed && passwordsMatch

    // Shake the card when they try to submit something invalid
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = rememberComicHaptic()
    val readyBurst = rememberBurstState()
    var celebratedReady by remember { mutableStateOf(false) }
    LaunchedEffect(formValid) {
        if (formValid && !celebratedReady) {
            celebratedReady = true
            readyBurst.fire("READY!", CGreenMint)
        }
        if (!formValid) celebratedReady = false
    }

    val coin = rememberCoinDrop()
    val successBurst = rememberBurstState()
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            successBurst.fire("WELCOME, PLAYER 2!", CGreenMint)
            Chiptune.play(Chiptune.Sfx.POWER_UP)
            kotlinx.coroutines.delay(800)
            authViewModel.resetAuthState()
            onAccountCreated()
        }
        if (authState is AuthState.Error) {
            repeat(6) { i -> shake.animateTo(if (i % 2 == 0) 14f else -14f, tween(45)) }
            shake.animateTo(0f, tween(45))
        }
    }

    // Subtle logo wobble
    val wobble by rememberGlowRange(2f, -2f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComicGlassBg)
    ) {
        FloatingInvadersBackground()
        // Decorative dots texture
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dotColor = CGreen.copy(alpha = 0.15f)
            listOf(40f to 100f, 300f to 60f, 60f to 500f, 320f to 360f,
                   160f to 180f, 280f to 650f, 80f to 700f, 340f to 240f).forEach { pair ->
                drawCircle(dotColor, radius = 6f, center = androidx.compose.ui.geometry.Offset(pair.first, pair.second))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // ── Logo sticker (powers on like an old CRT) ──────────────────────
            CrtPowerOn {
            Box(
                modifier = Modifier
                    .rotate(wobble)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CGreen)
                    .border(3.dp, ScrapbookDark, RoundedCornerShape(20.dp))
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "RETROHUB",
                    fontFamily = BangersFontFamily,
                    color = ScrapbookDark,
                    fontSize = 44.sp,
                    letterSpacing = 3.sp
                )
            }
            } // CrtPowerOn

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "▸ PLAYER 2 — JOIN NOW ◂",
                fontFamily = BangersFontFamily,
                color = ScrapbookDark.copy(alpha = 0.45f),
                fontSize = 13.sp,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Form card ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .dealIn(delayMs = 300)
                    .graphicsLayer { translationX = shake.value }
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(2.5.dp, ScrapbookDark, RoundedCornerShape(18.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "CREATE ACCOUNT",
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 26.sp,
                        letterSpacing = 2.sp
                    )

                    RetroAuthField(
                        value = username,
                        onValueChange = { username = it.replace(" ", "") },
                        label = "Username"
                    )
                    if (username.isNotEmpty()) {
                        FieldStatusLine(
                            error = usernameError,
                            availability = usernameAvail,
                            takenText = "That username is taken",
                            okText = "@${username.trim().lowercase()} is available!"
                        )
                    }

                    RetroAuthField(
                        value = email,
                        onValueChange = { email = it.trim() },
                        label = "Email",
                        keyboardType = KeyboardType.Email
                    )
                    if (email.isNotEmpty()) {
                        FieldStatusLine(
                            error = emailError,
                            availability = emailAvail,
                            takenText = "An account already uses this email",
                            okText = "Email looks good"
                        )
                    }

                    RetroAuthField(
                        value = password,
                        onValueChange = { password = it; passwordMismatch = false },
                        label = "Password",
                        isPassword = true
                    )
                    if (password.isNotEmpty()) PasswordRulesChecklist(pwCheck)

                    RetroAuthField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; passwordMismatch = false },
                        label = "Confirm Password",
                        isPassword = true
                    )
                    if (confirmPassword.isNotEmpty()) {
                        FieldStatusLine(
                            error = if (passwordsMatch) null else "Passwords don't match yet",
                            availability = Availability.AVAILABLE,
                            takenText = "",
                            okText = "Passwords match"
                        )
                    }

                    // Password mismatch warning
                    if (passwordMismatch) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CAcYellow.copy(alpha = 0.1f))
                                .border(1.dp, CAcYellow, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                "⚠ Passwords don't match!",
                                fontFamily = NunitoFontFamily,
                                color = CAcYellowD,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Auth error
                    if (authState is AuthState.Error) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CAcRed.copy(alpha = 0.1f))
                                .border(1.dp, CAcRed, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                "⚠ ${(authState as AuthState.Error).message}",
                                fontFamily = NunitoFontFamily,
                                color = CAcRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Create account button (coin drops in on tap)
                    Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    authState is AuthState.Loading -> CGreen.copy(alpha = 0.5f)
                                    formValid -> CGreen
                                    else -> ScrapbookDark.copy(alpha = 0.12f)
                                }
                            )
                            .border(2.dp, ScrapbookDark, RoundedCornerShape(12.dp))
                            .clickable(enabled = authState !is AuthState.Loading) {
                                if (password != confirmPassword) passwordMismatch = true
                                if (!formValid) {
                                    // wobble the card so it's obvious something needs fixing
                                    haptic()
                                    scope.launch {
                                        repeat(6) { i -> shake.animateTo(if (i % 2 == 0) 14f else -14f, tween(45)) }
                                        shake.animateTo(0f, tween(45))
                                    }
                                } else {
                                    coin.fire()
                                    authViewModel.createAccountWithEmail(email, password, username)
                                }
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(
                                color = ScrapbookDark,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                "▶  CREATE ACCOUNT",
                                fontFamily = BangersFontFamily,
                                color = if (formValid) ScrapbookDark else ScrapbookDark.copy(alpha = 0.45f),
                                fontSize = 18.sp,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                    CoinDrop(coin, Modifier.align(Alignment.Center))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sign in link
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onNavigateToLogin() }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 14.sp)) {
                            append("Already have an account?  ")
                        }
                        withStyle(SpanStyle(fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp,
                            letterSpacing = 1.sp, background = CGreen)) {
                            append("  SIGN IN ◂  ")
                        }
                    },
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
        ComicBurst(readyBurst, Modifier.align(Alignment.Center), burstSize = 130.dp)
        ComicBurst(successBurst, Modifier.align(Alignment.Center), burstSize = 180.dp)
    }
}

// ─── Status line under a field (✓ / ✗ / checking…) ────────────────────────────

@Composable
fun FieldStatusLine(error: String?, availability: Availability, takenText: String, okText: String) {
    val (icon, text, color) = when {
        error != null -> Triple("✗", error, CAcRed)
        availability == Availability.CHECKING -> Triple("…", "Checking…", ScrapbookTextMuted)
        availability == Availability.TAKEN -> Triple("✗", takenText, CAcRed)
        availability == Availability.AVAILABLE -> Triple("✓", okText, CGreenDeep)
        else -> Triple("✓", "Looks good", CGreenDeep)
    }
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(icon) { pop.snapTo(0.6f); pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 600f)) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp).offset(y = (-8).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(icon, fontFamily = BangersFontFamily, color = color, fontSize = 14.sp,
            modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value })
        Text(text, fontFamily = NunitoFontFamily, fontWeight = FontWeight.SemiBold, color = color, fontSize = 12.sp)
    }
}

// ─── Password requirements with animated ticks + strength bar ─────────────────

@Composable
fun PasswordRulesChecklist(check: SignupRules.PasswordCheck) {
    val rules = listOf(
        check.startsWithCapital to "Starts with a capital letter",
        check.longEnough to "At least ${SignupRules.PASSWORD_MIN} characters",
        check.hasNumber to "Contains a number",
        check.hasSpecial to "Contains a special character (!@#\$…)"
    )
    val passed = rules.count { it.first }
    val strength by animateFloatAsState(passed / 4f, spring(dampingRatio = 0.6f, stiffness = 300f), label = "pwStrength")
    val label = when (passed) { 0, 1 -> "WEAK"; 2 -> "OK"; 3 -> "GOOD"; else -> "STRONG" }

    Column(
        modifier = Modifier.fillMaxWidth().offset(y = (-6).dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ComicGlassBg)
            .border(1.5.dp, ScrapbookDark.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp))
                    .background(ScrapbookDark.copy(alpha = 0.1f))
            ) {
                Box(Modifier.fillMaxWidth(strength).fillMaxHeight()
                    .background(if (passed == 4) CGreen else if (passed >= 2) CGreenMint else CAcRed))
            }
            Spacer(Modifier.width(8.dp))
            Text(label, fontFamily = BangersFontFamily, fontSize = 12.sp,
                color = if (passed == 4) CGreenDeep else if (passed >= 2) ScrapbookDark else CAcRed)
        }
        rules.forEach { (ok, text) ->
            val tick = remember { Animatable(if (ok) 1f else 0f) }
            LaunchedEffect(ok) {
                if (ok) { tick.snapTo(0.3f); tick.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 650f)) }
                else tick.snapTo(1f)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier.size(16.dp)
                        .graphicsLayer { scaleX = tick.value; scaleY = tick.value }
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (ok) CGreen else Color.White)
                        .border(1.5.dp, ScrapbookDark, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) { if (ok) Text("✓", fontSize = 10.sp, color = ScrapbookDark, fontFamily = BangersFontFamily) }
                Text(text, fontFamily = NunitoFontFamily, fontSize = 12.sp,
                    color = if (ok) CGreenDeep else ScrapbookTextMuted,
                    fontWeight = if (ok) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

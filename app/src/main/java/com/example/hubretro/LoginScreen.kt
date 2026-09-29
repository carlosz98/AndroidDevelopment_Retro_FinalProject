package com.example.hubretro

import androidx.compose.ui.focus.onFocusChanged

import androidx.compose.ui.graphics.graphicsLayer

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hubretro.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit,
    onNavigateToCreateAccount: () -> Unit
) {
    val context   = LocalContext.current
    val authState by authViewModel.authState.collectAsState()

    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("362544702533-jae4a67e1l2lck7j5etpbdg69hj58mne.apps.googleusercontent.com")
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: androidx.activity.result.ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            if (intent != null) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
                try {
                    val account = task.getResult(ApiException::class.java)
                    val idToken = account?.idToken
                    if (idToken != null) authViewModel.signInWithGoogle(idToken)
                } catch (e: ApiException) { /* ignored */ }
            }
        }
    }

    val coin = rememberCoinDrop()
    val successBurst = rememberBurstState()
    val shake = remember { Animatable(0f) }
    val haptic = rememberComicHaptic()
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            // Little celebration before jumping into the app
            successBurst.fire("PLAYER 1 READY!", CGreenMint)
            Chiptune.play(Chiptune.Sfx.POWER_UP)
            kotlinx.coroutines.delay(750)
            authViewModel.resetAuthState()
            onLoginSuccess()
        }
    }

    // Subtle wobble on the logo sticker
    val wobble by rememberGlowRange(-2f, 2f)
    // Wrong password etc. → shake the card
    LaunchedEffect(authState) {
        if (authState is AuthState.Error) {
            haptic()
            repeat(6) { i -> shake.animateTo(if (i % 2 == 0) 14f else -14f, tween(45)) }
            shake.animateTo(0f, tween(45))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComicGlassBg)
    ) {
        FloatingInvadersBackground()
        // Scattered decorative dots (scrapbook texture)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dotColor = CGreen.copy(alpha = 0.15f)
            listOf(60f to 120f, 310f to 80f, 50f to 580f, 340f to 420f,
                   180f to 200f, 270f to 700f, 90f to 750f, 330f to 280f).forEach { pair ->
                drawCircle(dotColor, radius = 6f, center = androidx.compose.ui.geometry.Offset(pair.first, pair.second))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            // ── Logo sticker (powers on like an old CRT) ──────────────────────
            CrtPowerOn {
            Box(
                modifier = Modifier
                    .rotate(wobble)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CGreen)
                    .border(3.dp, ScrapbookDark, RoundedCornerShape(20.dp))
                    .padding(horizontal = 28.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "RETROHUB",
                    fontFamily = BangersFontFamily,
                    color = ScrapbookDark,
                    fontSize = 48.sp,
                    letterSpacing = 3.sp
                )
            }
            } // CrtPowerOn

            Spacer(modifier = Modifier.height(6.dp))
            ArcadeBlinkText(
                text = "▸ INSERT COIN — PLAYER 1 ◂",
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = BangersFontFamily,
                    color = ScrapbookDark.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    letterSpacing = 2.sp
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

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
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "SIGN IN",
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 28.sp,
                        letterSpacing = 2.sp
                    )

                    RetroAuthField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email",
                        keyboardType = KeyboardType.Email
                    )

                    RetroAuthField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        isPassword = true
                    )

                    // Error
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
                                text = "⚠ ${(authState as AuthState.Error).message}",
                                fontFamily = NunitoFontFamily,
                                color = CAcRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Sign in button (a coin drops in when you tap it)
                    Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (authState is AuthState.Loading) CGreen.copy(alpha = 0.5f) else CGreen)
                            .border(2.dp, ScrapbookDark, RoundedCornerShape(12.dp))
                            .clickable(enabled = authState !is AuthState.Loading) {
                                coin.fire()
                                authViewModel.signInWithEmail(email, password)
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
                                "▶  SIGN IN",
                                fontFamily = BangersFontFamily,
                                color = ScrapbookDark,
                                fontSize = 18.sp,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                    CoinDrop(coin, Modifier.align(Alignment.Center))
                    }

                    // Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = ScrapbookDark.copy(alpha = 0.15f))
                        Text("OR", fontFamily = BangersFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, letterSpacing = 2.sp)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = ScrapbookDark.copy(alpha = 0.15f))
                    }

                    // Google button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(2.dp, ScrapbookDark.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable(enabled = authState !is AuthState.Loading) {
                                googleLauncher.launch(googleSignInClient.signInIntent)
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("G", fontFamily = BangersFontFamily, color = CAcBlue, fontSize = 16.sp)
                            Text(
                                "CONTINUE WITH GOOGLE",
                                fontFamily = BangersFontFamily,
                                color = ScrapbookDark,
                                fontSize = 15.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Create account link
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onNavigateToCreateAccount() }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 14.sp)) {
                            append("New here?  ")
                        }
                        withStyle(SpanStyle(fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, letterSpacing = 1.sp,
                            background = CGreen)) {
                            append("  CREATE ACCOUNT ▸  ")
                        }
                    },
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
        ComicBurst(successBurst, Modifier.align(Alignment.Center), burstSize = 170.dp)
    }
}

// ─── Shared scrapbook auth input field ────────────────────────────────────────
@Composable
fun RetroAuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    accentColor: Color = CGreen
) {
    var focused by remember { mutableStateOf(false) }
    val lift by animateFloatAsState(if (focused) 1f else 0f,
        spring(dampingRatio = 0.55f, stiffness = 420f), label = "fieldFocusLift")
    Box(modifier = modifier.fillMaxWidth()) {
    // Hard green shadow that pops out when the field is focused
    Box(
        modifier = Modifier.matchParentSize().padding(top = 8.dp)
            .graphicsLayer { translationX = lift * 4.dp.toPx(); translationY = lift * 4.dp.toPx(); alpha = lift }
            .clip(RoundedCornerShape(10.dp)).background(accentColor)
    )
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                fontFamily = BangersFontFamily,
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )
        },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = TextStyle(
            fontFamily = NunitoFontFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = ScrapbookDark
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = ScrapbookDark,
            unfocusedTextColor = ScrapbookDark,
            focusedBorderColor = ScrapbookDark,
            unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedLabelColor = ScrapbookDark,
            unfocusedLabelColor = ScrapbookTextMuted,
            cursorColor = ScrapbookDark
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
            .graphicsLayer { translationX = -lift * 1.dp.toPx(); translationY = -lift * 1.dp.toPx() }
            .onFocusChanged { focused = it.isFocused }
    )
    }
}

// Alias so existing callers still compile
@Composable
fun ScrapbookAuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) = RetroAuthField(value, onValueChange, label, modifier, isPassword, keyboardType)

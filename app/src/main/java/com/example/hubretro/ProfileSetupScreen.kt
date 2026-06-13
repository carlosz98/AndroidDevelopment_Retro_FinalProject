package com.example.hubretro

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import kotlinx.coroutines.delay

// ─── Platform accent colors ───────────────────────────────────────────────────
val PsnBlue = Color(0xFF003791)
val XboxGreen = Color(0xFF107C10)
val SteamBlue = Color(0xFF1B2838)
val NintendoRed = Color(0xFFE4000F)

// ─── Data ─────────────────────────────────────────────────────────────────────
data class ProfileSetupData(
    val username: String = "",
    val profilePictureUri: Uri? = null,
    val bannerUri: Uri? = null,
    val selectedGames: List<IGDBGame> = emptyList(),
    val selectedSoundtracks: List<IGDBSoundtrack> = emptyList(),
    val psnUsername: String = "",
    val xboxUsername: String = "",
    val steamUsername: String = "",
    val nintendoUsername: String = "",
    val habboUsername: String = "",
    val habboRegion: String = "habbo.com"
)

// ─── Habbo regions ────────────────────────────────────────────────────────────
val setupHabboRegions = listOf(
    "habbo.com" to "🌍 Global",
    "habbo.es" to "🇪🇸 Spain",
    "habbo.com.br" to "🇧🇷 Brazil",
    "habbo.de" to "🇩🇪 Germany",
    "habbo.fi" to "🇫🇮 Finland",
    "habbo.fr" to "🇫🇷 France",
    "habbo.it" to "🇮🇹 Italy",
    "habbo.nl" to "🇳🇱 Netherlands",
    "habbo.com.tr" to "🇹🇷 Turkey",
    "habbo.dk" to "🇩🇰 Denmark",
    "habbo.no" to "🇳🇴 Norway",
    "habbo.se" to "🇸🇪 Sweden"
)

// ─── Main Screen ──────────────────────────────────────────────────────────────
@Composable
fun ProfileSetupScreen(
    authViewModel: AuthViewModel = viewModel(),
    onSetupComplete: () -> Unit
) {
    var currentStep by remember { mutableStateOf(1) }
    var goingForward by remember { mutableStateOf(true) }
    var setupData by remember { mutableStateOf(ProfileSetupData()) }
    var showWelcome by remember { mutableStateOf(false) }

    val totalSteps = 6
    val stepLabels = listOf("NAME", "PHOTOS", "GAMES", "MUSIC", "PLATFORMS", "HABBO")
    val stepEmojis = listOf("✏️", "📸", "🎮", "🎵", "🕹️", "🏨")

    val neonT = rememberInfiniteTransition(label = "setupNeon")
    val neonAlpha by neonT.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse),
        label = "setupNeonAlpha"
    )

    // ✅ Welcome celebration overlay
    if (showWelcome) {
        WelcomeCelebration(onDone = onSetupComplete)
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(ScrapbookCream)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ✅ Golden gradient header
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(colors = listOf(ScrapbookYellow, Color(0xFFFFE566), ScrapbookYellow)))
                    .border(BorderStroke(2.dp, ScrapbookBorder))
                    .padding(top = 48.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                // Scan line
                val scanT = rememberInfiniteTransition(label = "setupScan")
                val scanX by scanT.animateFloat(initialValue = -400f, targetValue = 400f, animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "setupScanX")
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter).background(Brush.horizontalGradient(colors = listOf(Color.Transparent, ScrapbookDark.copy(alpha = 0.15f), Color.Transparent), startX = scanX, endX = scanX + 200f)))

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    // Shimmer title
                    val shimmerT = rememberInfiniteTransition(label = "setupShimmer")
                    val shimmerX by shimmerT.animateFloat(initialValue = -300f, targetValue = 600f, animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart), label = "setupShimmerX")
                    Box {
                        Text("SET UP PROFILE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 30.sp, letterSpacing = 2.sp, textAlign = TextAlign.Center)
                        Text("SET UP PROFILE", fontFamily = BangersFontFamily, fontSize = 30.sp, letterSpacing = 2.sp, textAlign = TextAlign.Center,
                            style = TextStyle(brush = Brush.linearGradient(colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.6f), Color.Transparent), start = androidx.compose.ui.geometry.Offset(shimmerX - 100f, 0f), end = androidx.compose.ui.geometry.Offset(shimmerX + 100f, 0f))))
                    }
                    Text("Let's build your retro identity 🕹️", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    // ✅ Step indicators with neon glow
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        (1..totalSteps).forEachIndexed { index, step ->
                            val isActive = currentStep == step
                            val isDone = currentStep > step
                            val stepScale by animateFloatAsState(targetValue = if (isActive) 1.2f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "stepScale_$step")

                            Box(
                                modifier = Modifier.scale(stepScale).size(if (isActive) 36.dp else 28.dp).clip(CircleShape)
                                    .background(if (isDone || isActive) ScrapbookDark else ScrapbookDark.copy(alpha = 0.2f))
                                    .then(if (isActive) Modifier.border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = CircleShape) else Modifier.border(2.dp, ScrapbookBorder, CircleShape)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDone) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = ScrapbookYellow, modifier = Modifier.size(16.dp))
                                } else {
                                    Text(stepEmojis[index], fontSize = if (isActive) 14.sp else 11.sp)
                                }
                            }

                            if (index < totalSteps - 1) {
                                Box(modifier = Modifier.width(20.dp).height(2.dp).background(if (currentStep > step) ScrapbookDark else ScrapbookDark.copy(alpha = 0.2f)))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Step labels
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        stepLabels.forEachIndexed { index, label ->
                            Text(label, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = if (currentStep == index + 1) ScrapbookDark else ScrapbookDark.copy(alpha = 0.35f), fontSize = 8.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            // ✅ Step content with directional slide transition
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (goingForward) {
                        (slideInHorizontally { it } + fadeIn(tween(300))) togetherWith (slideOutHorizontally { -it } + fadeOut(tween(200)))
                    } else {
                        (slideInHorizontally { -it } + fadeIn(tween(300))) togetherWith (slideOutHorizontally { it } + fadeOut(tween(200)))
                    }
                },
                label = "setup_step_transition",
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    1 -> UsernameStep(authViewModel = authViewModel, initialUsername = setupData.username, onNext = { username -> goingForward = true; setupData = setupData.copy(username = username); currentStep = 2 })
                    2 -> ProfilePhotoStep(initialProfileUri = setupData.profilePictureUri, initialBannerUri = setupData.bannerUri, onNext = { profileUri, bannerUri -> goingForward = true; setupData = setupData.copy(profilePictureUri = profileUri, bannerUri = bannerUri); currentStep = 3 }, onBack = { goingForward = false; currentStep = 1 })
                    3 -> TopGamesStep(selectedGames = setupData.selectedGames, onNext = { games -> goingForward = true; setupData = setupData.copy(selectedGames = games); currentStep = 4 }, onBack = { goingForward = false; currentStep = 2 })
                    4 -> TopSoundtracksStep(selectedSoundtracks = setupData.selectedSoundtracks, onNext = { soundtracks -> goingForward = true; setupData = setupData.copy(selectedSoundtracks = soundtracks); currentStep = 5 }, onBack = { goingForward = false; currentStep = 3 })
                    5 -> GamingPlatformsStep(initialPsn = setupData.psnUsername, initialXbox = setupData.xboxUsername, initialSteam = setupData.steamUsername, initialNintendo = setupData.nintendoUsername, onNext = { psn, xbox, steam, nintendo -> goingForward = true; setupData = setupData.copy(psnUsername = psn, xboxUsername = xbox, steamUsername = steam, nintendoUsername = nintendo); currentStep = 6 }, onBack = { goingForward = false; currentStep = 4 })
                    6 -> HabboSetupStep(initialUsername = setupData.habboUsername, initialRegion = setupData.habboRegion, onComplete = { habbo, region -> setupData = setupData.copy(habboUsername = habbo, habboRegion = region); authViewModel.completeProfileSetup(setupData); showWelcome = true }, onBack = { goingForward = false; currentStep = 5 })
                }
            }
        }
    }
}

// ─── Welcome Celebration ──────────────────────────────────────────────────────
@Composable
fun WelcomeCelebration(onDone: () -> Unit) {
    val neonT = rememberInfiniteTransition(label = "welcomeNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse), label = "welcomeNeonAlpha")
    val titleScale by animateFloatAsState(targetValue = 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "welcomeTitleScale")

    // Auto-navigate after 3 seconds
    LaunchedEffect(Unit) { delay(3200); onDone() }

    Box(
        modifier = Modifier.fillMaxSize().background(ScrapbookDark),
        contentAlignment = Alignment.Center
    ) {
        // Confetti dots
        val confettiColors = listOf(ScrapbookYellow, Color(0xFFFF6B6B), Color(0xFF4ECDC4), Color(0xFFFF8E53), Color(0xFFA8E6CF), Color.White)
        repeat(40) { i ->
            val confettiT = rememberInfiniteTransition(label = "confetti_$i")
            val confettiY by confettiT.animateFloat(initialValue = (-50 + i * 7 % 120).toFloat(), targetValue = (800 + i * 5 % 200).toFloat(), animationSpec = infiniteRepeatable(tween(1200 + i * 80, easing = LinearEasing), RepeatMode.Restart), label = "confettiY_$i")
            val confettiX = (i * 47 % 400).toFloat()
            val confettiSize = if (i % 3 == 0) 10.dp else if (i % 3 == 1) 6.dp else 8.dp
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
                Box(modifier = Modifier.padding(start = confettiX.dp, top = confettiY.dp).size(confettiSize).clip(if (i % 2 == 0) CircleShape else RoundedCornerShape(2.dp)).background(confettiColors[i % confettiColors.size]))
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎮", fontSize = 72.sp, modifier = Modifier.scale(titleScale))
            Spacer(modifier = Modifier.height(16.dp))
            Box {
                Text("WELCOME TO", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 28.sp, letterSpacing = 2.sp, textAlign = TextAlign.Center)
                Box(modifier = Modifier.matchParentSize().blur(8.dp).background(ScrapbookYellow.copy(alpha = 0.2f)))
            }
            Box {
                Text("RETROHUB!", fontFamily = BangersFontFamily, color = Color.White, fontSize = 48.sp, letterSpacing = 2.sp, textAlign = TextAlign.Center)
                Box(modifier = Modifier.matchParentSize().blur(12.dp).background(ScrapbookYellow.copy(alpha = neonAlpha * 0.3f)))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Your retro identity is ready 🕹️", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp, textAlign = TextAlign.Center)
        }
    }
}

// ─── Shared container ─────────────────────────────────────────────────────────
@Composable
fun SetupStepContainer(title: String, subtitle: String, emoji: String = "🎮", content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(20.dp))
        val emojiScale by rememberInfiniteTransition(label = "stepEmoji").animateFloat(initialValue = 1f, targetValue = 1.08f, animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOut), RepeatMode.Reverse), label = "stepEmojiScale")
        Text(emoji, fontSize = 36.sp, modifier = Modifier.scale(emojiScale))
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 26.sp, letterSpacing = 1.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(modifier = Modifier.height(16.dp))
        content()
    }
}

// ─── Shared nav buttons ───────────────────────────────────────────────────────
@Composable
fun SetupNavButtons(
    onBack: (() -> Unit)? = null,
    onNext: () -> Unit,
    nextLabel: String = "NEXT →",
    nextEnabled: Boolean = true,
    neonAlpha: Float = 0.6f
) {
    val neonT = rememberInfiniteTransition(label = "navNeon")
    val localNeon by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "navNeonAlpha")
    val alpha = if (neonAlpha > 0f) neonAlpha else localNeon

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (onBack != null) {
            var backPressed by remember { mutableStateOf(false) }
            val backScale by animateFloatAsState(targetValue = if (backPressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "backScale")
            Box(modifier = Modifier.weight(1f).scale(backScale).clip(RoundedCornerShape(12.dp)).background(ScrapbookPaper).border(2.dp, ScrapbookBorder, RoundedCornerShape(12.dp)).clickable { backPressed = true; onBack() }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                Text("← BACK", fontFamily = BangersFontFamily, fontSize = 18.sp, color = ScrapbookDark)
            }
            LaunchedEffect(backPressed) { if (backPressed) { delay(150); backPressed = false } }
        }
        var nextPressed by remember { mutableStateOf(false) }
        val nextScale by animateFloatAsState(targetValue = if (nextPressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "nextScale")
        Box(
            modifier = Modifier.weight(if (onBack != null) 1f else 2f).scale(nextScale)
                .clip(RoundedCornerShape(12.dp))
                .background(if (nextEnabled) ScrapbookDark else ScrapbookDark.copy(alpha = 0.3f))
                .then(if (nextEnabled) Modifier.border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = alpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = alpha))), shape = RoundedCornerShape(12.dp)) else Modifier.border(2.dp, ScrapbookBorder, RoundedCornerShape(12.dp)))
                .clickable(enabled = nextEnabled) { nextPressed = true; onNext() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(nextLabel, fontFamily = BangersFontFamily, fontSize = 18.sp, color = ScrapbookYellow)
        }
        LaunchedEffect(nextPressed) { if (nextPressed) { delay(150); nextPressed = false } }
    }
}

// ─── Step 1: Username ─────────────────────────────────────────────────────────
@Composable
fun UsernameStep(authViewModel: AuthViewModel, initialUsername: String, onNext: (String) -> Unit) {
    var username by remember { mutableStateOf(initialUsername) }
    var isChecking by remember { mutableStateOf(false) }
    var isAvailable by remember { mutableStateOf<Boolean?>(null) }

    // Typewriter placeholder cycling
    val placeholders = listOf("retro_mario_99", "sonic_fan_94", "zelda_master", "pac_man_king", "megaman_x")
    var placeholderIndex by remember { mutableStateOf(0) }
    var placeholderText by remember { mutableStateOf(placeholders[0]) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2200)
            placeholderIndex = (placeholderIndex + 1) % placeholders.size
            placeholderText = placeholders[placeholderIndex]
        }
    }

    LaunchedEffect(username) {
        if (username.length >= 3) {
            delay(600); isChecking = true
            isAvailable = authViewModel.checkUsernameAvailable(username)
            isChecking = false
        } else { isAvailable = null }
    }

    val neonT = rememberInfiniteTransition(label = "usernameNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "usernameNeonAlpha")

    SetupStepContainer(title = "YOUR USERNAME", subtitle = "Pick a unique name for your RetroHub profile", emoji = "✏️") {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {

            // ✅ Username field with neon border when available
            Box(
                modifier = Modifier.fillMaxWidth()
                    .then(if (isAvailable == true) Modifier.border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookGreen.copy(alpha = neonAlpha), ScrapbookGreen.copy(alpha = 0.3f), ScrapbookGreen.copy(alpha = neonAlpha))), shape = RoundedCornerShape(12.dp)) else Modifier)
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.lowercase().replace(" ", "").take(20); isAvailable = null },
                    placeholder = {
                        AnimatedContent(targetState = placeholderText, transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) }, label = "placeholder") { text ->
                            Text(text, fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextMuted)
                        }
                    },
                    leadingIcon = { Text("@", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, modifier = Modifier.padding(start = 8.dp)) },
                    trailingIcon = {
                        when {
                            isChecking -> CircularProgressIndicator(color = ScrapbookYellowDark, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            isAvailable == true -> Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ScrapbookGreen, modifier = Modifier.size(24.dp))
                            isAvailable == false -> Icon(Icons.Filled.Close, contentDescription = null, tint = ScrapbookRed, modifier = Modifier.size(24.dp))
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 16.sp, color = ScrapbookTextDark),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = when { isAvailable == true -> ScrapbookGreen; isAvailable == false -> ScrapbookRed; else -> ScrapbookYellow }, unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f), focusedContainerColor = ScrapbookCardWhite, unfocusedContainerColor = ScrapbookCardWhite, cursorColor = ScrapbookDark, focusedTextColor = ScrapbookTextDark, unfocusedTextColor = ScrapbookTextDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            // Character counter + hint
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    when {
                        username.length in 1..2 -> "⚠️ At least 3 characters"
                        isAvailable == true -> "✓ @$username is available!"
                        isAvailable == false -> "✗ @$username is taken"
                        else -> "No spaces, lowercase only 🕹️"
                    },
                    fontFamily = NunitoFontFamily,
                    color = when { isAvailable == true -> ScrapbookGreen; isAvailable == false -> ScrapbookRed; else -> ScrapbookTextMuted },
                    fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookDark.copy(alpha = 0.1f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("${username.length}/20", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            SetupNavButtons(onNext = { onNext(username) }, nextEnabled = isAvailable == true)
        }
    }
}

// ─── Step 2: Profile Photos ───────────────────────────────────────────────────
@Composable
fun ProfilePhotoStep(initialProfileUri: Uri?, initialBannerUri: Uri?, onNext: (Uri?, Uri?) -> Unit, onBack: () -> Unit) {
    var profileUri by remember { mutableStateOf(initialProfileUri) }
    var bannerUri by remember { mutableStateOf(initialBannerUri) }
    val profileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> profileUri = uri }
    val bannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> bannerUri = uri }

    val neonT = rememberInfiniteTransition(label = "photoNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "photoNeonAlpha")

    SetupStepContainer(title = "PROFILE PHOTOS", subtitle = "Set your look — you can always change it later", emoji = "📸") {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {

            // ✅ Live profile card preview
            Text("PREVIEW", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(12.dp))
                    .border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = RoundedCornerShape(12.dp))
            ) {
                // Banner
                if (bannerUri != null) {
                    AsyncImage(model = bannerUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(colors = listOf(ScrapbookDark, Color(0xFF1A1A2E)))))
                }
                // Dark overlay
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                // Avatar overlaid
                Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 12.dp)) {
                    Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(ScrapbookPaper)
                        .border(width = 3.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.4f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileUri != null) {
                            AsyncImage(model = profileUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = ScrapbookDark.copy(alpha = 0.3f), modifier = Modifier.size(28.dp))
                        }
                    }
                }
                // Name placeholder
                Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = 84.dp, bottom = 14.dp)) {
                    Text("YOUR NAME", fontFamily = BangersFontFamily, color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Banner picker
            Text("BANNER IMAGE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(10.dp)).background(ScrapbookPaper).border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp)).clickable { bannerLauncher.launch("image/*") }, contentAlignment = Alignment.Center) {
                if (bannerUri != null) {
                    AsyncImage(model = bannerUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text("CHANGE BANNER", fontFamily = BangersFontFamily, color = Color.White, fontSize = 13.sp)
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = ScrapbookDark.copy(alpha = 0.4f), modifier = Modifier.size(24.dp))
                        Text("TAP TO ADD BANNER", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.4f), fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Profile pic picker
            Text("PROFILE PICTURE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(ScrapbookPaper)
                    .border(width = 3.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = CircleShape)
                    .clickable { profileLauncher.launch("image/*") }, contentAlignment = Alignment.Center
                ) {
                    if (profileUri != null) {
                        AsyncImage(model = profileUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = ScrapbookDark.copy(alpha = 0.3f), modifier = Modifier.size(28.dp))
                            Text("TAP", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.4f), fontSize = 9.sp)
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (profileUri != null) "✓ Photo selected!" else "Tap the circle to pick your photo", fontFamily = NunitoFontFamily, color = if (profileUri != null) ScrapbookGreen else ScrapbookTextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Recommended: square image", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SetupNavButtons(onBack = onBack, onNext = { onNext(profileUri, bannerUri) }, nextEnabled = true)

            // Skip option
            Spacer(modifier = Modifier.height(8.dp))
            Text("SKIP FOR NOW", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.35f), fontSize = 13.sp, modifier = Modifier.clickable { onNext(null, null) }, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─── Step 3: Top Games ────────────────────────────────────────────────────────
@Composable
fun TopGamesStep(selectedGames: List<IGDBGame>, onNext: (List<IGDBGame>) -> Unit, onBack: () -> Unit) {
    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<IGDBGame>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(selectedGames.toMutableList()) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) { delay(600); isSearching = true; searchResults = IGDBRepository.searchGames(searchQuery); isSearching = false }
        else { searchResults = emptyList() }
    }

    SetupStepContainer(title = "TOP 6 GAMES", subtitle = "Pick up to 6 all-time favorites", emoji = "🎮") {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Counter pill
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pick your legends 🕹️", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (selected.size == 6) ScrapbookGreen else ScrapbookDark).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("${selected.size}/6", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                placeholder = { Text("Search for a game...", fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextMuted) },
                leadingIcon = { if (isSearching) CircularProgressIndicator(color = ScrapbookYellowDark, modifier = Modifier.size(20.dp).padding(2.dp), strokeWidth = 2.dp) else Icon(Icons.Filled.Search, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(20.dp)) },
                trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Filled.Close, contentDescription = null, tint = ScrapbookTextMuted, modifier = Modifier.size(18.dp)) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextDark),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScrapbookYellow, unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f), focusedContainerColor = ScrapbookCardWhite, unfocusedContainerColor = ScrapbookCardWhite, cursorColor = ScrapbookDark, focusedTextColor = ScrapbookTextDark, unfocusedTextColor = ScrapbookTextDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (selected.isNotEmpty()) {
                LazyVerticalGrid(columns = GridCells.Fixed(6), contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().height(80.dp)) {
                    items(selected, key = { it.id }) { game ->
                        var removePressed by remember { mutableStateOf(false) }
                        val removeScale by animateFloatAsState(targetValue = if (removePressed) 0.85f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "removeGame")
                        Box(modifier = Modifier.aspectRatio(3f / 4f).scale(removeScale).clip(RoundedCornerShape(6.dp)).background(ScrapbookPaper).border(2.dp, ScrapbookYellow, RoundedCornerShape(6.dp)).clickable { removePressed = true; selected = selected.toMutableList().also { it.remove(game) } }) {
                            if (game.coverUrl != null) AsyncImage(model = game.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)), contentAlignment = Alignment.TopEnd) {
                                Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp).padding(1.dp))
                            }
                        }
                        LaunchedEffect(removePressed) { if (removePressed) { delay(150); removePressed = false } }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(searchResults, key = { it.id }) { game ->
                    val isSelected = selected.any { it.id == game.id }
                    var addPressed by remember { mutableStateOf(false) }
                    val addScale by animateFloatAsState(targetValue = if (addPressed) 0.96f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "addGame")
                    Box(modifier = Modifier.scale(addScale)) {
                        GameSearchResultItem(game = game, isSelected = isSelected, onToggle = {
                            addPressed = true
                            if (isSelected) selected = selected.toMutableList().also { list -> list.removeAll { it.id == game.id } }
                            else if (selected.size < 6) selected = selected.toMutableList().also { it.add(game) }
                        })
                    }
                    LaunchedEffect(addPressed) { if (addPressed) { delay(150); addPressed = false } }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            SetupNavButtons(onBack = onBack, onNext = { onNext(selected) }, nextEnabled = selected.isNotEmpty(), nextLabel = if (selected.isEmpty()) "SELECT A GAME" else "NEXT →")
            Spacer(modifier = Modifier.height(4.dp))
            Text("SKIP FOR NOW", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.35f), fontSize = 13.sp, modifier = Modifier.fillMaxWidth().clickable { onNext(emptyList()) }.padding(bottom = 8.dp), textAlign = TextAlign.Center)
        }
    }
}

// ─── Step 4: Soundtracks ──────────────────────────────────────────────────────
@Composable
fun TopSoundtracksStep(selectedSoundtracks: List<IGDBSoundtrack>, onNext: (List<IGDBSoundtrack>) -> Unit, onBack: () -> Unit) {
    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<IGDBSoundtrack>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(selectedSoundtracks.toMutableList()) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) { delay(600); isSearching = true; searchResults = IGDBRepository.searchSoundtracks(searchQuery); isSearching = false }
        else { searchResults = emptyList() }
    }

    SetupStepContainer(title = "TOP 3 SOUNDTRACKS", subtitle = "Pick the OSTs that define you 🎵", emoji = "🎵") {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Your sonic identity 🎶", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (selected.size == 3) ScrapbookGreen else ScrapbookDark).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("${selected.size}/3", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                placeholder = { Text("Search for a game soundtrack...", fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextMuted) },
                leadingIcon = { if (isSearching) CircularProgressIndicator(color = ScrapbookYellowDark, modifier = Modifier.size(20.dp).padding(2.dp), strokeWidth = 2.dp) else Icon(Icons.Filled.Search, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(20.dp)) },
                trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Filled.Close, contentDescription = null, tint = ScrapbookTextMuted, modifier = Modifier.size(18.dp)) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextDark),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScrapbookYellow, unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f), focusedContainerColor = ScrapbookCardWhite, unfocusedContainerColor = ScrapbookCardWhite, cursorColor = ScrapbookDark, focusedTextColor = ScrapbookTextDark, unfocusedTextColor = ScrapbookTextDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            // ✅ Spinning vinyl discs
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) { index ->
                    val soundtrack = selected.getOrNull(index)
                    val vinylRotation by rememberInfiniteTransition(label = "vinyl_$index").animateFloat(
                        initialValue = 0f, targetValue = if (soundtrack != null) 360f else 0f,
                        animationSpec = if (soundtrack != null) infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart) else infiniteRepeatable(tween(1000), RepeatMode.Restart),
                        label = "vinylRotate_$index"
                    )
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f).rotate(if (soundtrack != null) vinylRotation else 0f).clip(CircleShape)
                        .background(if (soundtrack != null) ScrapbookDark else ScrapbookPaper)
                        .border(if (soundtrack != null) 3.dp else 2.dp, if (soundtrack != null) ScrapbookYellow else ScrapbookBorder, CircleShape)
                        .then(if (soundtrack != null) Modifier.clickable { selected = selected.toMutableList().also { it.remove(soundtrack) } } else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        if (soundtrack?.coverUrl != null) {
                            AsyncImage(model = soundtrack.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                        // Vinyl hole
                        Box(modifier = Modifier.size(if (soundtrack != null) 16.dp else 24.dp).background(if (soundtrack != null) ScrapbookCardWhite else ScrapbookDark.copy(alpha = 0.15f), CircleShape).border(1.dp, ScrapbookBorder, CircleShape))
                        if (soundtrack != null) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f)), contentAlignment = Alignment.TopEnd) {
                                Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp).padding(1.dp))
                            }
                        } else {
                            Text("${index + 1}", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.25f), fontSize = 20.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(searchResults, key = { it.id }) { soundtrack ->
                    val isSelected = selected.any { it.id == soundtrack.id }
                    SoundtrackSearchResultItem(soundtrack = soundtrack, isSelected = isSelected, onToggle = {
                        if (isSelected) selected = selected.toMutableList().also { list -> list.removeAll { it.id == soundtrack.id } }
                        else if (selected.size < 3) selected = selected.toMutableList().also { it.add(soundtrack) }
                    })
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            SetupNavButtons(onBack = onBack, onNext = { onNext(selected) }, nextEnabled = selected.isNotEmpty(), nextLabel = if (selected.isEmpty()) "SELECT A SOUNDTRACK" else "NEXT →")
            Spacer(modifier = Modifier.height(4.dp))
            Text("SKIP FOR NOW", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.35f), fontSize = 13.sp, modifier = Modifier.fillMaxWidth().clickable { onNext(emptyList()) }.padding(bottom = 8.dp), textAlign = TextAlign.Center)
        }
    }
}

// ─── Step 5: Gaming Platforms ─────────────────────────────────────────────────
@Composable
fun GamingPlatformsStep(initialPsn: String, initialXbox: String, initialSteam: String, initialNintendo: String, onNext: (String, String, String, String) -> Unit, onBack: () -> Unit) {
    var psn by remember { mutableStateOf(initialPsn) }
    var xbox by remember { mutableStateOf(initialXbox) }
    var steam by remember { mutableStateOf(initialSteam) }
    var nintendo by remember { mutableStateOf(initialNintendo) }

    SetupStepContainer(title = "GAMING PLATFORMS", subtitle = "Let friends find you everywhere — all optional!", emoji = "🕹️") {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("All fields are optional 👾", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

            PlatformSetupField(value = psn, onValueChange = { psn = it }, platform = gamingPlatforms[0], accentColor = PsnBlue)
            PlatformSetupField(value = xbox, onValueChange = { xbox = it }, platform = gamingPlatforms[1], accentColor = XboxGreen)
            PlatformSetupField(value = steam, onValueChange = { steam = it }, platform = gamingPlatforms[2], accentColor = SteamBlue)
            PlatformSetupField(value = nintendo, onValueChange = { nintendo = it }, platform = gamingPlatforms[3], accentColor = NintendoRed)

            // Live preview
            val activePlatforms = listOf(gamingPlatforms[0] to psn, gamingPlatforms[1] to xbox, gamingPlatforms[2] to steam, gamingPlatforms[3] to nintendo).filter { (_, u) -> u.isNotBlank() }
            if (activePlatforms.isNotEmpty()) {
                Text("PREVIEW", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(activePlatforms) { (platform, username) -> PlatformBubble(platform = platform, username = username) }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            SetupNavButtons(onBack = onBack, onNext = { onNext(psn.trim(), xbox.trim(), steam.trim(), nintendo.trim()) }, nextEnabled = true)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─── Step 6: Habbo Hotel ──────────────────────────────────────────────────────
@Composable
fun HabboSetupStep(initialUsername: String, initialRegion: String, onComplete: (String, String) -> Unit, onBack: () -> Unit) {
    var habboUsername by remember { mutableStateOf(initialUsername) }
    var selectedRegion by remember { mutableStateOf(initialRegion) }
    var liveUsername by remember { mutableStateOf(initialUsername) }

    val neonT = rememberInfiniteTransition(label = "habboNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "habboNeonAlpha")

    // Debounce live preview
    LaunchedEffect(habboUsername, selectedRegion) { delay(800); liveUsername = habboUsername }

    SetupStepContainer(title = "HABBO HOTEL", subtitle = "Link your Habbo avatar to your profile 🏨", emoji = "🏨") {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {

            // ✅ Live Habbo avatar preview
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ScrapbookDark)
                    .border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (liveUsername.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Habbo avatar image
                        val avatarUrl = "https://$selectedRegion/habbo-imaging/avatarimage?user=$liveUsername&action=std&direction=2&head_direction=2&gesture=sml&size=l"
                        Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(ScrapbookPaper.copy(alpha = 0.1f))) {
                            AsyncImage(model = avatarUrl, contentDescription = "Habbo Avatar", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                        }
                        Column {
                            Text(liveUsername, fontFamily = BangersFontFamily, color = Color.White, fontSize = 20.sp)
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookYellow.copy(alpha = 0.2f)).border(1.dp, ScrapbookYellow.copy(alpha = 0.4f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("🏨 HABBO", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏨", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Enter your username to see\nyour avatar here!", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Username field
            OutlinedTextField(
                value = habboUsername,
                onValueChange = { habboUsername = it },
                placeholder = { Text("Your Habbo username", fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextMuted) },
                leadingIcon = { Text("🏨", fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp)) },
                trailingIcon = { if (habboUsername.isNotBlank()) IconButton(onClick = { habboUsername = "" }) { Icon(Icons.Filled.Close, contentDescription = null, tint = ScrapbookTextMuted, modifier = Modifier.size(16.dp)) } },
                singleLine = true,
                textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextDark),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScrapbookYellow, unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f), focusedContainerColor = ScrapbookCardWhite, unfocusedContainerColor = ScrapbookCardWhite, cursorColor = ScrapbookDark, focusedTextColor = ScrapbookTextDark, unfocusedTextColor = ScrapbookTextDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Region selector
            Text("SELECT YOUR SERVER", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(setupHabboRegions) { (domain, label) ->
                    val isSelected = selectedRegion == domain
                    var pressed by remember { mutableStateOf(false) }
                    val chipScale by animateFloatAsState(targetValue = if (pressed) 0.92f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "regionChip")
                    Box(
                        modifier = Modifier.scale(chipScale).clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) ScrapbookDark else ScrapbookCardWhite)
                            .then(if (isSelected) Modifier.border(width = 1.5.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = RoundedCornerShape(20.dp)) else Modifier.border(1.dp, ScrapbookBorder, RoundedCornerShape(20.dp)))
                            .clickable { pressed = true; selectedRegion = domain }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(label, fontFamily = BangersFontFamily, color = if (isSelected) ScrapbookYellow else ScrapbookDark, fontSize = 12.sp)
                    }
                    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("This shows your pixel avatar across the app 🎮", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(20.dp))
            SetupNavButtons(onBack = onBack, onNext = { onComplete(habboUsername.trim(), selectedRegion) }, nextLabel = "FINISH ✓", nextEnabled = true)
            Spacer(modifier = Modifier.height(4.dp))
            Text("SKIP FOR NOW", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.35f), fontSize = 13.sp, modifier = Modifier.fillMaxWidth().clickable { onComplete("", selectedRegion) }.padding(bottom = 16.dp), textAlign = TextAlign.Center)
        }
    }
}

// ─── Platform Setup Field ─────────────────────────────────────────────────────
@Composable
fun PlatformSetupField(value: String, onValueChange: (String) -> Unit, platform: GamingPlatform, accentColor: Color = ScrapbookDark) {
    val neonT = rememberInfiniteTransition(label = "platformNeon_${platform.name}")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "platformNeonAlpha")
    val isFilled = value.isNotBlank()

    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
            Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(if (isFilled) accentColor else ScrapbookDark).border(2.dp, ScrapbookBorder, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                Image(painter = painterResource(id = platform.iconResId), contentDescription = platform.name, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(platform.name.uppercase(), fontFamily = BangersFontFamily, color = if (isFilled) accentColor else ScrapbookDark, fontSize = 15.sp)
            if (isFilled) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ScrapbookGreen, modifier = Modifier.size(16.dp))
            }
        }
        Box(modifier = Modifier.fillMaxWidth().then(if (isFilled) Modifier.border(width = 1.5.dp, brush = Brush.linearGradient(colors = listOf(accentColor.copy(alpha = neonAlpha * 0.7f), accentColor.copy(alpha = 0.2f), accentColor.copy(alpha = neonAlpha * 0.7f))), shape = RoundedCornerShape(10.dp)) else Modifier)) {
            OutlinedTextField(
                value = value, onValueChange = onValueChange,
                placeholder = { Text("Your ${platform.name} username", fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookTextMuted) },
                leadingIcon = { Text("@", fontFamily = BangersFontFamily, color = if (isFilled) accentColor else ScrapbookDark, fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp)) },
                trailingIcon = { if (value.isNotBlank()) IconButton(onClick = { onValueChange("") }) { Icon(Icons.Filled.Close, contentDescription = null, tint = ScrapbookTextMuted, modifier = Modifier.size(16.dp)) } },
                singleLine = true,
                textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextDark),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f), focusedContainerColor = ScrapbookCardWhite, unfocusedContainerColor = ScrapbookCardWhite, cursorColor = accentColor, focusedTextColor = ScrapbookTextDark, unfocusedTextColor = ScrapbookTextDark),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ─── Game Search Result Item ──────────────────────────────────────────────────
@Composable
fun GameSearchResultItem(game: IGDBGame, isSelected: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth().clickable { onToggle() }, backgroundColor = if (isSelected) ScrapbookYellow.copy(alpha = 0.2f) else ScrapbookCardWhite, cornerRadius = 10.dp, shadowOffset = 2.dp) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(ScrapbookPaper).border(2.dp, if (isSelected) ScrapbookYellow else ScrapbookBorder, RoundedCornerShape(6.dp))) {
                    if (game.coverUrl != null) AsyncImage(model = game.coverUrl, contentDescription = game.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(game.name, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    game.releaseYear?.let { Text(it.toString(), fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp) }
                }
                if (isSelected) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ScrapbookGreen, modifier = Modifier.size(24.dp))
            }
        }
    }
}

// ─── Soundtrack Search Result Item ────────────────────────────────────────────
@Composable
fun SoundtrackSearchResultItem(soundtrack: IGDBSoundtrack, isSelected: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth().clickable { onToggle() }, backgroundColor = if (isSelected) ScrapbookYellow.copy(alpha = 0.2f) else ScrapbookCardWhite, cornerRadius = 10.dp, shadowOffset = 2.dp) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(ScrapbookDark).border(2.dp, if (isSelected) ScrapbookYellow else ScrapbookBorder, CircleShape), contentAlignment = Alignment.Center) {
                    if (soundtrack.coverUrl != null) AsyncImage(model = soundtrack.coverUrl, contentDescription = soundtrack.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(modifier = Modifier.size(12.dp).background(ScrapbookCardWhite, CircleShape))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(soundtrack.name, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    soundtrack.gameName?.let { Text(it, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                if (isSelected) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ScrapbookGreen, modifier = Modifier.size(24.dp))
            }
        }
    }
}
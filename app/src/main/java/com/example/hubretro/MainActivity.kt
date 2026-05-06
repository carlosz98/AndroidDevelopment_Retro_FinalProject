package com.example.hubretro

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hubretro.ui.theme.HubRetroTheme
import com.example.hubretro.ui.theme.RetroFontFamily
import com.example.hubretro.ui.theme.VaporwavePink
import com.example.hubretro.utils.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class TopActionItem(
    val label: String,
    val route: String
)

val drawerNavItems = listOf(
    TopActionItem("HOME", "home"),
    TopActionItem("DISCOVER", "discover"),
    TopActionItem("MAGAZINES", "magazines"),
    TopActionItem("ALBUMS", "albums"),
    TopActionItem("ARTICLES", "articles"),
    TopActionItem("PROFILE", "profile")
)

val robotMessages = mapOf(
    "HOME" to listOf(
        "Welcome to RetroHub! Blast from the past, eh?",
        "Ready to explore some vintage vibes?",
        "Don't forget to check out the latest oldies!"
    ),
    "DISCOVER" to listOf(
        "Looking for something specific?",
        "Search across all of RetroHub!",
        "Find users, magazines, albums and more!"
    ),
    "MAGAZINES" to listOf(
        "Flipping through digital pages of history.",
        "So many classic articles and ads!",
        "Found any cool retro tips in the magazines?"
    ),
    "ALBUMS" to listOf(
        "Spinning some classic digital tracks!",
        "Which album art is your favorite?",
        "Crank up the volume... well, metaphorically."
    ),
    "ARTICLES" to listOf(
        "Deep dive into retro tech and culture.",
        "Learn something new about the good ol' days.",
        "These articles are a trip down memory lane."
    ),
    "PROFILE" to listOf(
        "Checking out your retro cred, are we?",
        "Customize your experience, time traveler!",
        "This is your corner of the retroverse."
    ),
    "DEFAULT" to listOf(
        "Hey there, retro enthusiast!",
        "Navigating the neon-lit corridors of time...",
        "What shall we explore today?"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SoundManager.initialize(applicationContext)

        // Enable true fullscreen edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            HubRetroTheme {
                // --- Shared ViewModels ---
                val authViewModel: AuthViewModel = viewModel()
                val favoritesViewModel: FavoritesViewModel = viewModel()
                val activityViewModel: ActivityViewModel = viewModel()
                val userArticlesViewModel: UserArticlesViewModel = viewModel()
                val achievementsViewModel: AchievementsViewModel = viewModel()
                val retroRadioViewModel: RetroRadioViewModel = viewModel()
                val currentUser by authViewModel.currentUser.collectAsState()

                // Wire activity logging into favorites
                favoritesViewModel.activityViewModel = activityViewModel
                // Wire activity logging into auth (for join + follow events)
                authViewModel.activityViewModel = activityViewModel
                // ✅ Wire XP gains into activity logging
                activityViewModel.achievementsViewModel = achievementsViewModel

                // Refresh achievements when user changes
                LaunchedEffect(currentUser?.uid) {
                    if (currentUser != null) {
                        achievementsViewModel.refreshForUser()
                    }
                }

                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var selectedActionLabel by remember { mutableStateOf(drawerNavItems.first().label) }
                var showCreateAccount by remember { mutableStateOf(false) }

                LaunchedEffect(selectedActionLabel) {
                    if (selectedActionLabel != "PROFILE") {
                        showCreateAccount = false
                    }
                }

                var robotVisible by remember { mutableStateOf(false) }
                var robotMessage by remember { mutableStateOf("") }
                var currentMessageIndex by remember { mutableStateOf(0) }

                LaunchedEffect(Unit) {
                    while (true) {
                        delay(20000L)
                        if (!robotVisible) {
                            val messagesForScreen =
                                robotMessages[selectedActionLabel.uppercase()]
                                    ?: robotMessages["DEFAULT"]!!
                            robotMessage =
                                messagesForScreen[currentMessageIndex % messagesForScreen.size]
                            currentMessageIndex++
                            robotVisible = true
                            delay(7000L)
                            robotVisible = false
                        }
                    }
                }

                LaunchedEffect(selectedActionLabel) {
                    if (!robotVisible) {
                        val messagesForScreen =
                            robotMessages[selectedActionLabel.uppercase()]
                                ?: robotMessages["DEFAULT"]!!
                        robotMessage = messagesForScreen.random()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.my_retro_background),
                        contentDescription = "Retro background image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(
                                drawerContainerColor = Color(0xFF2A2A3D).copy(alpha = 0.95f)
                            ) {
                                Spacer(Modifier.height(20.dp))
                                drawerNavItems.forEach { item ->
                                    NavigationDrawerItem(
                                        label = {
                                            Text(
                                                item.label,
                                                fontFamily = RetroFontFamily,
                                                fontSize = 18.sp
                                            )
                                        },
                                        selected = item.label == selectedActionLabel,
                                        onClick = {
                                            SoundManager.playSound(SoundManager.SOUND_NAVIGATION_TAP)
                                            if (selectedActionLabel != item.label) {
                                                selectedActionLabel = item.label
                                            }
                                            scope.launch { drawerState.close() }
                                        },
                                        modifier = Modifier.padding(
                                            horizontal = 16.dp,
                                            vertical = 8.dp
                                        ),
                                        colors = NavigationDrawerItemDefaults.colors(
                                            selectedTextColor = VaporwavePink,
                                            selectedContainerColor = Color.Black.copy(alpha = 0.2f),
                                            unselectedTextColor = Color.White,
                                            unselectedContainerColor = Color.Transparent
                                        )
                                    )
                                }
                                Spacer(Modifier.height(20.dp))
                            }
                        }
                    ) {
                        Scaffold(
                            containerColor = Color.Transparent,
                            topBar = {
                                Box(modifier = Modifier.padding(top = 55.dp)) {
                                    RetroAppBar(
                                        currentScreenLabel = selectedActionLabel,
                                        onNavigationIconClick = {
                                            SoundManager.playSound(SoundManager.SOUND_NAVIGATION_TAP)
                                            scope.launch {
                                                if (drawerState.isClosed) drawerState.open()
                                                else drawerState.close()
                                            }
                                        }
                                    )
                                }
                            }
                        ) { innerPadding ->

                            // --- Capture ViewModels before AnimatedContent scope ---
                            val capturedAuthViewModel = authViewModel
                            val capturedFavoritesViewModel = favoritesViewModel
                            val capturedActivityViewModel = activityViewModel
                            val capturedUserArticlesViewModel = userArticlesViewModel
                            val capturedAchievementsViewModel = achievementsViewModel

                            AnimatedContent(
                                targetState = selectedActionLabel,
                                modifier = Modifier
                                    .padding(innerPadding)
                                    .padding(bottom = 60.dp), // ✅ space for radio bar
                                transitionSpec = {
                                    val duration = 600
                                    val exitTransition =
                                        scaleOut(
                                            animationSpec = tween(
                                                durationMillis = duration - 100,
                                                easing = FastOutLinearInEasing
                                            ),
                                            targetScale = 0.3f
                                        ) + fadeOut(
                                            animationSpec = tween(
                                                durationMillis = duration,
                                                delayMillis = 50,
                                                easing = LinearEasing
                                            )
                                        )
                                    val enterTransition =
                                        scaleIn(
                                            animationSpec = tween(
                                                durationMillis = duration - 100,
                                                delayMillis = 50,
                                                easing = LinearOutSlowInEasing
                                            ),
                                            initialScale = 0.3f
                                        ) + fadeIn(
                                            animationSpec = tween(
                                                durationMillis = duration,
                                                easing = LinearEasing
                                            )
                                        )
                                    ContentTransform(
                                        targetContentEnter = enterTransition,
                                        initialContentExit = exitTransition
                                    )
                                },
                                label = "PixelateScreenTransition"
                            ) { targetScreenLabel ->
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Log.d(
                                        "ScreenSelection",
                                        "AnimatedContent rendering for: $targetScreenLabel"
                                    )
                                    when (targetScreenLabel.uppercase()) {
                                        "HOME" -> HomeScreen(
                                            onNavigateToAlbums = {
                                                SoundManager.playSound(SoundManager.SOUND_BUTTON_PRIMARY_CLICK)
                                                selectedActionLabel = "ALBUMS"
                                            },
                                            onNavigateToMagazines = {
                                                SoundManager.playSound(SoundManager.SOUND_BUTTON_PRIMARY_CLICK)
                                                selectedActionLabel = "MAGAZINES"
                                            },
                                            onNavigateToArticles = {
                                                SoundManager.playSound(SoundManager.SOUND_BUTTON_PRIMARY_CLICK)
                                                selectedActionLabel = "ARTICLES"
                                            },
                                            onNavigateToProfile = {
                                                SoundManager.playSound(SoundManager.SOUND_BUTTON_PRIMARY_CLICK)
                                                selectedActionLabel = "PROFILE"
                                            }
                                        )
                                        "DISCOVER" -> DiscoverScreen(
                                            authViewModel = capturedAuthViewModel
                                        )
                                        "MAGAZINES" -> MagazinesScreen(
                                            favoritesViewModel = capturedFavoritesViewModel
                                        )
                                        "ALBUMS" -> AlbumsScreen(
                                            favoritesViewModel = capturedFavoritesViewModel
                                        )
                                        "ARTICLES" -> ArticlesScreen(
                                            favoritesViewModel = capturedFavoritesViewModel,
                                            authViewModel = capturedAuthViewModel,
                                            activityViewModel = capturedActivityViewModel,
                                            userArticlesViewModel = capturedUserArticlesViewModel
                                        )

                                        // --- PROFILE TAB WITH AUTH FLOW ---
                                        "PROFILE" -> {
                                            if (currentUser != null) {
                                                val profile by capturedAuthViewModel.userProfile.collectAsState()
                                                if (profile?.setupComplete == true) {
                                                    ProfileScreen(
                                                        authViewModel = capturedAuthViewModel,
                                                        favoritesViewModel = capturedFavoritesViewModel,
                                                        activityViewModel = capturedActivityViewModel,
                                                        achievementsViewModel = capturedAchievementsViewModel
                                                    )
                                                } else {
                                                    ProfileSetupScreen(
                                                        authViewModel = capturedAuthViewModel,
                                                        onSetupComplete = { }
                                                    )
                                                }
                                            } else if (showCreateAccount) {
                                                CreateAccountScreen(
                                                    authViewModel = capturedAuthViewModel,
                                                    onAccountCreated = { showCreateAccount = false },
                                                    onNavigateToLogin = { showCreateAccount = false }
                                                )
                                            } else {
                                                LoginScreen(
                                                    authViewModel = capturedAuthViewModel,
                                                    onLoginSuccess = { },
                                                    onNavigateToCreateAccount = { showCreateAccount = true }
                                                )
                                            }
                                        }
                                        // --- END PROFILE TAB ---

                                        else -> {
                                            Log.w(
                                                "ScreenSelection",
                                                "Unexpected screen label '$targetScreenLabel', defaulting to HOME."
                                            )
                                            HomeScreen(
                                                onNavigateToAlbums = { selectedActionLabel = "ALBUMS" },
                                                onNavigateToMagazines = { selectedActionLabel = "MAGAZINES" },
                                                onNavigateToArticles = { selectedActionLabel = "ARTICLES" },
                                                onNavigateToProfile = { selectedActionLabel = "PROFILE" }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ✅ Retro Radio bubble — bottom left
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            TalkingRobot(
                                message = robotMessage,
                                isVisible = robotVisible,
                                robotSpriteResId = R.drawable.robot,
                                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                            )
                            RetroRadioPlayer(
                                radioViewModel = retroRadioViewModel,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundManager.release()
    }
}

@Composable
fun RetroAppBar(
    currentScreenLabel: String,
    onNavigationIconClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigationIconClick) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Open Navigation Menu",
                tint = Color.White
            )
        }
        Text(
            text = currentScreenLabel.uppercase(),
            color = Color.White,
            fontFamily = RetroFontFamily,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
    }
}
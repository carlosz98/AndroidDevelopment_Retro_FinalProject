package com.example.hubretro

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
// Animation imports
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
// End of animation imports
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme // Keep this if HubRetroTheme uses it
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hubretro.ui.theme.HubRetroTheme
import com.example.hubretro.ui.theme.RetroFontFamily
import com.example.hubretro.ui.theme.VaporwavePink
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Import screens
import com.example.hubretro.MagazinesScreen
import com.example.hubretro.ArticlesScreen
import com.example.hubretro.AlbumsScreen
import com.example.hubretro.HomeScreen
import com.example.hubretro.ProfileScreen

// Import TalkingRobot
import com.example.hubretro.TalkingRobot


data class TopActionItem(
    val label: String,
    val route: String
)

val drawerNavItems = listOf(
    TopActionItem("HOME", "home"),
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
        setContent {
            HubRetroTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var selectedActionLabel by remember { mutableStateOf(drawerNavItems.first().label) }

                var robotVisible by remember { mutableStateOf(false) }
                var robotMessage by remember { mutableStateOf("") }
                var currentMessageIndex by remember { mutableStateOf(0) }

                LaunchedEffect(Unit) {
                    while (true) {
                        delay(20000L) // Show message every 20 seconds
                        if (!robotVisible) {
                            val messagesForScreen =
                                robotMessages[selectedActionLabel.uppercase()] ?: robotMessages["DEFAULT"]!!
                            robotMessage =
                                messagesForScreen[currentMessageIndex % messagesForScreen.size]
                            currentMessageIndex++
                            robotVisible = true
                            delay(7000L) // Keep message visible for 7 seconds
                            robotVisible = false
                        }
                    }
                }

                LaunchedEffect(selectedActionLabel) {
                    // Update message immediately if screen changes and robot is not visible
                    if (!robotVisible) {
                        val messagesForScreen =
                            robotMessages[selectedActionLabel.uppercase()] ?: robotMessages["DEFAULT"]!!
                        robotMessage = messagesForScreen.random()
                        // Optionally make it visible for a short period on screen change
                        // robotVisible = true
                        // delay(5000L)
                        // robotVisible = false
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
                                            if (selectedActionLabel != item.label) {
                                                selectedActionLabel = item.label
                                            }
                                            scope.launch { drawerState.close() }
                                        },
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
                                Box(modifier = Modifier.padding(top = 55.dp)) { // Added padding to push AppBar down
                                    RetroAppBar(
                                        currentScreenLabel = selectedActionLabel,
                                        onNavigationIconClick = {
                                            scope.launch {
                                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                            }
                                        }
                                    )
                                }
                            }
                        ) { innerPadding ->
                            AnimatedContent(
                                targetState = selectedActionLabel,
                                modifier = Modifier.padding(innerPadding),
                                transitionSpec = {
                                    val duration = 600
                                    val exitTransition =
                                        scaleOut(
                                            animationSpec = tween(durationMillis = duration - 100, easing = FastOutLinearInEasing),
                                            targetScale = 0.3f
                                        ) + fadeOut(
                                            animationSpec = tween(durationMillis = duration, delayMillis = 50, easing = LinearEasing)
                                        )
                                    val enterTransition =
                                        scaleIn(
                                            animationSpec = tween(durationMillis = duration - 100, delayMillis = 50, easing = LinearOutSlowInEasing),
                                            initialScale = 0.3f
                                        ) + fadeIn(
                                            animationSpec = tween(durationMillis = duration, easing = LinearEasing)
                                        )
                                    ContentTransform(targetContentEnter = enterTransition, initialContentExit = exitTransition)
                                },
                                label = "PixelateScreenTransition"
                            ) { targetScreenLabel ->
                                Box(modifier = Modifier.fillMaxSize()) { // Ensure content Box fills available space
                                    Log.d("ScreenSelection", "AnimatedContent rendering for: $targetScreenLabel")
                                    when (targetScreenLabel.uppercase()) {
                                        "ARTICLES" -> ArticlesScreen()
                                        "HOME" -> HomeScreen(
                                            onNavigateToAlbums = { selectedActionLabel = "ALBUMS" },
                                            onNavigateToMagazines = { selectedActionLabel = "MAGAZINES" },
                                            onNavigateToArticles = { selectedActionLabel = "ARTICLES" },
                                            onNavigateToProfile = { selectedActionLabel = "PROFILE" }
                                        )
                                        "MAGAZINES" -> MagazinesScreen()
                                        "ALBUMS" -> AlbumsScreen()
                                        "PROFILE" -> ProfileScreen()
                                        else -> {
                                            Log.w("ScreenSelection", "Unexpected screen label '$targetScreenLabel', defaulting to HOME.")
                                            HomeScreen( // Default to HomeScreen or a placeholder
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

                    // TalkingRobot call
                    TalkingRobot(
                        message = robotMessage,
                        isVisible = robotVisible,
                        robotSpriteResId = R.drawable.robot, // ENSURE R.drawable.robot EXISTS!
                        modifier = Modifier
                            .align(Alignment.BottomStart) // CHANGED TO BOTTOM-LEFT
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MainTitle(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Yellow // Default text color
) {
    Text(
        text = text.uppercase(),
        color = textColor,
        fontFamily = RetroFontFamily,
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
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
            .padding(horizontal = 8.dp, vertical = 16.dp), // Adjusted padding slightly
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigationIconClick) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Open Navigation Menu",
                tint = Color.White // Explicitly White
            )
        }
        Text(
            text = currentScreenLabel.uppercase(),
            color = Color.White, // Explicitly White
            fontFamily = RetroFontFamily,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        // Potentially add a Spacer or another IconButton here if needed for balance, e.g. Spacer(Modifier.width(48.dp))
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF2A2A3D)
@Composable
fun DefaultPreview() {
    HubRetroTheme {
        var selectedPreviewScreenLabel by remember { mutableStateOf(drawerNavItems.firstOrNull()?.label ?: "HOME") }
        val robotPreviewMessage = "Previewing RetroHub!"

        Box(Modifier.fillMaxSize()) { // Preview Box fills size
            Scaffold(
                containerColor = Color(0xFF2A2A3D), // Consistent preview background
                topBar = {
                    Box(modifier = Modifier.padding(top = 10.dp)) {
                        RetroAppBar(
                            currentScreenLabel = selectedPreviewScreenLabel,
                            onNavigationIconClick = { Log.d("Preview", "Nav icon clicked.") }
                        )
                    }
                }
            ) { innerPadding ->
                AnimatedContent(
                    targetState = selectedPreviewScreenLabel,
                    modifier = Modifier.padding(innerPadding),
                    transitionSpec = { // Simplified transition for preview
                        ContentTransform(
                            targetContentEnter = fadeIn(animationSpec = tween(300)),
                            initialContentExit = fadeOut(animationSpec = tween(300))
                        )
                    },
                    label = "PreviewScreenTransition"
                ) { targetPreviewLabel ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        when (targetPreviewLabel.uppercase()) {
                            "ARTICLES" -> MainTitle(text = "Articles Screen Preview")
                            "MAGAZINES" -> MainTitle(text = "Magazines Screen Preview")
                            "ALBUMS" -> MainTitle(text = "Albums Screen Preview")
                            "HOME" -> MainTitle(text = "Home Screen Preview")
                            "PROFILE" -> MainTitle(text = "Profile Screen Preview")
                            else -> MainTitle(text = "Preview: $targetPreviewLabel")
                        }
                    }
                }
            }

            TalkingRobot(
                message = robotPreviewMessage,
                isVisible = true, // Always visible for preview
                robotSpriteResId = null, // Uses placeholder icon in preview
                modifier = Modifier
                    .align(Alignment.BottomStart) // CHANGED TO BOTTOM-LEFT for preview consistency
                    .padding(16.dp)
            )
        }
    }
}


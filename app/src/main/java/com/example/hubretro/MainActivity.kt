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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hubretro.ui.theme.*
import com.example.hubretro.utils.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class TopActionItem(
    val label: String,
    val route: String
)

// Bottom nav tabs
data class BottomNavItem(
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("HOME", Icons.Filled.Home),
    BottomNavItem("DISCOVER", Icons.Filled.Explore),
    BottomNavItem("MESSAGES", Icons.Filled.Chat),
    BottomNavItem("PROFILE", Icons.Filled.Person)
)

// Drawer — secondary content nav only
val drawerNavItems = listOf(
    TopActionItem("MAGAZINES", "magazines"),
    TopActionItem("ALBUMS", "albums"),
    TopActionItem("ARTICLES", "articles")
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
    "MESSAGES" to listOf(
        "Got messages waiting for you!",
        "Stay connected with the community!",
        "Slide into those DMs!"
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
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            HubRetroTheme {
                val authViewModel: AuthViewModel = viewModel()
                val favoritesViewModel: FavoritesViewModel = viewModel()
                val activityViewModel: ActivityViewModel = viewModel()
                val userArticlesViewModel: UserArticlesViewModel = viewModel()
                val achievementsViewModel: AchievementsViewModel = viewModel()
                val retroRadioViewModel: RetroRadioViewModel = viewModel()
                val chatViewModel: ChatViewModel = viewModel()
                val currentUser by authViewModel.currentUser.collectAsState()
                val totalUnread by chatViewModel.totalUnread.collectAsState()

                favoritesViewModel.activityViewModel = activityViewModel
                authViewModel.activityViewModel = activityViewModel
                activityViewModel.achievementsViewModel = achievementsViewModel

                LaunchedEffect(currentUser?.uid) {
                    if (currentUser != null) {
                        achievementsViewModel.refreshForUser()
                        chatViewModel.listenToChatRooms()
                    }
                }

                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                // Bottom nav manages main screen
                var selectedTab by remember { mutableStateOf("HOME") }
                // Secondary label for drawer content screens
                var selectedContentLabel by remember { mutableStateOf("") }
                var showCreateAccount by remember { mutableStateOf(false) }

                // Chat navigation state
                var activeChatRoom by remember { mutableStateOf<ChatRoom?>(null) }
                var showNewChat by remember { mutableStateOf(false) }

                // Current label for robot messages
                val currentLabel = if (selectedContentLabel.isNotBlank()) selectedContentLabel
                else selectedTab

                var robotVisible by remember { mutableStateOf(false) }
                var robotMessage by remember { mutableStateOf("") }
                var currentMessageIndex by remember { mutableStateOf(0) }

                LaunchedEffect(Unit) {
                    while (true) {
                        delay(20000L)
                        if (!robotVisible) {
                            val msgs = robotMessages[currentLabel.uppercase()]
                                ?: robotMessages["DEFAULT"]!!
                            robotMessage = msgs[currentMessageIndex % msgs.size]
                            currentMessageIndex++
                            robotVisible = true
                            delay(7000L)
                            robotVisible = false
                        }
                    }
                }

                LaunchedEffect(currentLabel) {
                    if (!robotVisible) {
                        val msgs = robotMessages[currentLabel.uppercase()]
                            ?: robotMessages["DEFAULT"]!!
                        robotMessage = msgs.random()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ScrapbookCream)
                ) {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(drawerContainerColor = ScrapbookCream) {
                                // Drawer header
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ScrapbookYellow)
                                        .border(BorderStroke(2.dp, ScrapbookBorder))
                                        .padding(vertical = 24.dp, horizontal = 16.dp)
                                ) {
                                    Text(
                                        text = "CONTENT",
                                        fontFamily = BangersFontFamily,
                                        fontSize = 36.sp,
                                        color = ScrapbookDark,
                                        letterSpacing = 2.sp
                                    )
                                }

                                Spacer(Modifier.height(12.dp))

                                drawerNavItems.forEach { item ->
                                    NavigationDrawerItem(
                                        label = {
                                            Text(
                                                item.label,
                                                fontFamily = BangersFontFamily,
                                                fontSize = 22.sp,
                                                letterSpacing = 1.sp
                                            )
                                        },
                                        selected = item.label == selectedContentLabel,
                                        onClick = {
                                            SoundManager.playSound(SoundManager.SOUND_NAVIGATION_TAP)
                                            selectedContentLabel = item.label
                                            selectedTab = ""
                                            scope.launch { drawerState.close() }
                                        },
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp, vertical = 4.dp
                                        ),
                                        colors = NavigationDrawerItemDefaults.colors(
                                            selectedTextColor = ScrapbookDark,
                                            selectedContainerColor = ScrapbookYellow,
                                            unselectedTextColor = ScrapbookTextDark,
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
                                // Hide top bar when in chat screens
                                val showingChat = selectedTab == "MESSAGES"
                                if (!showingChat || selectedContentLabel.isNotBlank()) {
                                    Box(modifier = Modifier.padding(top = 40.dp)) {
                                        RetroAppBar(
                                            currentScreenLabel = if (selectedContentLabel.isNotBlank())
                                                selectedContentLabel else selectedTab,
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
                            },
                            bottomBar = {
                                // Bottom nav bar
                                ScrapbookBottomNav(
                                    selectedTab = selectedTab,
                                    onTabSelected = { tab ->
                                        SoundManager.playSound(SoundManager.SOUND_NAVIGATION_TAP)
                                        selectedTab = tab
                                        selectedContentLabel = ""
                                        activeChatRoom = null
                                        showNewChat = false
                                    },
                                    totalUnread = totalUnread
                                )
                            }
                        ) { innerPadding ->

                            Box(
                                modifier = Modifier
                                    .padding(innerPadding)
                                    .fillMaxSize()
                                    .background(ScrapbookCream)
                            ) {
                                // Content screen
                                val screenKey = if (selectedContentLabel.isNotBlank())
                                    selectedContentLabel else selectedTab

                                AnimatedContent(
                                    targetState = screenKey,
                                    modifier = Modifier.fillMaxSize(),
                                    transitionSpec = {
                                        val duration = 600
                                        ContentTransform(
                                            targetContentEnter = scaleIn(
                                                tween(duration - 100, 50, LinearOutSlowInEasing),
                                                0.3f
                                            ) + fadeIn(tween(duration, easing = LinearEasing)),
                                            initialContentExit = scaleOut(
                                                tween(duration - 100, easing = FastOutLinearInEasing),
                                                0.3f
                                            ) + fadeOut(tween(duration, 50, LinearEasing))
                                        )
                                    },
                                    label = "ScreenTransition"
                                ) { target ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(ScrapbookCream)
                                    ) {
                                        when (target.uppercase()) {
                                            "HOME" -> HomeScreen(
                                                onNavigateToAlbums = {
                                                    SoundManager.playSound(SoundManager.SOUND_BUTTON_PRIMARY_CLICK)
                                                    selectedContentLabel = "ALBUMS"
                                                    selectedTab = ""
                                                },
                                                onNavigateToMagazines = {
                                                    SoundManager.playSound(SoundManager.SOUND_BUTTON_PRIMARY_CLICK)
                                                    selectedContentLabel = "MAGAZINES"
                                                    selectedTab = ""
                                                },
                                                onNavigateToArticles = {
                                                    SoundManager.playSound(SoundManager.SOUND_BUTTON_PRIMARY_CLICK)
                                                    selectedContentLabel = "ARTICLES"
                                                    selectedTab = ""
                                                },
                                                onNavigateToProfile = {
                                                    SoundManager.playSound(SoundManager.SOUND_BUTTON_PRIMARY_CLICK)
                                                    selectedTab = "PROFILE"
                                                    selectedContentLabel = ""
                                                }
                                            )
                                            "DISCOVER" -> DiscoverScreen(
                                                authViewModel = authViewModel
                                            )
                                            "MESSAGES" -> {
                                                when {
                                                    activeChatRoom != null -> ChatScreen(
                                                        chatRoom = activeChatRoom!!,
                                                        chatViewModel = chatViewModel,
                                                        authViewModel = authViewModel,
                                                        onBack = { activeChatRoom = null }
                                                    )
                                                    showNewChat -> NewChatScreen(
                                                        chatViewModel = chatViewModel,
                                                        authViewModel = authViewModel,
                                                        onChatCreated = { chatId ->
                                                            showNewChat = false
                                                            // Find the room and open it
                                                            val state = chatViewModel.chatRooms.value
                                                            if (state is ChatUiState.Success) {
                                                                activeChatRoom = state.rooms
                                                                    .firstOrNull { it.id == chatId }
                                                            }
                                                        },
                                                        onBack = { showNewChat = false }
                                                    )
                                                    else -> ChatListScreen(
                                                        chatViewModel = chatViewModel,
                                                        authViewModel = authViewModel,
                                                        onOpenChat = { room ->
                                                            activeChatRoom = room
                                                        },
                                                        onNewChat = { showNewChat = true }
                                                    )
                                                }
                                            }
                                            "MAGAZINES" -> MagazinesScreen(
                                                favoritesViewModel = favoritesViewModel
                                            )
                                            "ALBUMS" -> AlbumsScreen(
                                                favoritesViewModel = favoritesViewModel
                                            )
                                            "ARTICLES" -> ArticlesScreen(
                                                favoritesViewModel = favoritesViewModel,
                                                authViewModel = authViewModel,
                                                activityViewModel = activityViewModel,
                                                userArticlesViewModel = userArticlesViewModel
                                            )
                                            "PROFILE" -> {
                                                if (currentUser != null) {
                                                    val profile by authViewModel.userProfile.collectAsState()
                                                    if (profile?.setupComplete == true) {
                                                        ProfileScreen(
                                                            authViewModel = authViewModel,
                                                            favoritesViewModel = favoritesViewModel,
                                                            activityViewModel = activityViewModel,
                                                            achievementsViewModel = achievementsViewModel
                                                        )
                                                    } else {
                                                        ProfileSetupScreen(
                                                            authViewModel = authViewModel,
                                                            onSetupComplete = { }
                                                        )
                                                    }
                                                } else if (showCreateAccount) {
                                                    CreateAccountScreen(
                                                        authViewModel = authViewModel,
                                                        onAccountCreated = { showCreateAccount = false },
                                                        onNavigateToLogin = { showCreateAccount = false }
                                                    )
                                                } else {
                                                    LoginScreen(
                                                        authViewModel = authViewModel,
                                                        onLoginSuccess = { },
                                                        onNavigateToCreateAccount = { showCreateAccount = true }
                                                    )
                                                }
                                            }
                                            else -> HomeScreen(
                                                onNavigateToAlbums = { selectedContentLabel = "ALBUMS" },
                                                onNavigateToMagazines = { selectedContentLabel = "MAGAZINES" },
                                                onNavigateToArticles = { selectedContentLabel = "ARTICLES" },
                                                onNavigateToProfile = { selectedTab = "PROFILE" }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Radio + Robot — only show when not in chat
                    if (activeChatRoom == null && !showNewChat) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(bottom = 64.dp) // above bottom nav
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
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundManager.release()
    }
}

// ✅ Scrapbook Bottom Nav Bar
@Composable
fun ScrapbookBottomNav(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    totalUnread: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScrapbookYellow)
            .border(BorderStroke(2.dp, ScrapbookBorder))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        bottomNavItems.forEach { item ->
            val isSelected = selectedTab == item.label
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(item.label) }
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) ScrapbookDark
                            else ScrapbookDark.copy(alpha = 0.4f),
                            modifier = Modifier.size(if (isSelected) 28.dp else 24.dp)
                        )
                        // Unread badge on MESSAGES
                        if (item.label == "MESSAGES" && totalUnread > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-4).dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(ScrapbookDark)
                                    .border(1.dp, ScrapbookBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (totalUnread > 9) "9+" else "$totalUnread",
                                    fontFamily = BangersFontFamily,
                                    color = ScrapbookYellow,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                    Text(
                        text = item.label,
                        fontFamily = BangersFontFamily,
                        color = if (isSelected) ScrapbookDark
                        else ScrapbookDark.copy(alpha = 0.4f),
                        fontSize = if (isSelected) 13.sp else 12.sp
                    )
                    // Selection dot
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) ScrapbookDark
                                else Color.Transparent
                            )
                    )
                }
            }
        }
    }
}

// ✅ Scrapbook App Bar
@Composable
fun RetroAppBar(
    currentScreenLabel: String,
    onNavigationIconClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ScrapbookYellow)
            .border(BorderStroke(2.dp, ScrapbookBorder))
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigationIconClick) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Open Navigation Menu",
                tint = ScrapbookDark
            )
        }
        Text(
            text = currentScreenLabel.uppercase(),
            color = ScrapbookDark,
            fontFamily = BangersFontFamily,
            fontSize = 24.sp,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
    }
}
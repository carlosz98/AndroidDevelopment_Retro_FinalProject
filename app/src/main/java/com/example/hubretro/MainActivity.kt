package com.example.hubretro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hubretro.ui.theme.*
import com.example.hubretro.utils.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class TopActionItem(val label: String, val route: String)
data class BottomNavItem(val label: String, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem("HOME", Icons.Filled.Home),
    BottomNavItem("DISCOVER", Icons.Filled.Explore),
    BottomNavItem("MESSAGES", Icons.Filled.Chat),
    BottomNavItem("PROFILE", Icons.Filled.Person)
)

val drawerNavItems = listOf(
    TopActionItem("MAGAZINES", "magazines"),
    TopActionItem("ALBUMS", "albums"),
    TopActionItem("ARTICLES", "articles"),
    TopActionItem("STREAMS", "streams"),
    TopActionItem("GAMES", "games"),
    TopActionItem("EVENTS", "events"),
    TopActionItem("MARKETPLACE", "marketplace")
)

val drawerNavIcons = mapOf(
    "MAGAZINES" to Icons.Filled.MenuBook,
    "ALBUMS" to Icons.Filled.Album,
    "ARTICLES" to Icons.Filled.Article,
    "STREAMS" to Icons.Filled.LiveTv,
    "GAMES" to Icons.Filled.SportsEsports,
    "EVENTS" to Icons.Filled.Event,
    "MARKETPLACE" to Icons.Filled.Store
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
    "STREAMS" to listOf(
        "Someone's live right now playing retro games!",
        "Check out the latest retro gaming streams!",
        "Twitch and YouTube retro content, all in one place!"
    ),
    "PROFILE" to listOf(
        "Checking out your retro cred, are we?",
        "Customize your experience, time traveler!",
        "This is your corner of the retroverse."
    ),
    "GAMES" to listOf(
        "Browse the retro game database!",
        "Find your favorite classic games!",
        "Powered by IGDB — millions of games!"
    ),
    "EVENTS" to listOf(
        "Check out retro gaming anniversaries!",
        "Any community events coming up?",
        "Today in retro gaming history!"
    ),
    "MARKETPLACE" to listOf(
        "Looking for retro games to buy or trade?",
        "List your games for the community!",
        "Find retro gems in the marketplace!"
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
                val streamsViewModel: StreamsViewModel = viewModel()
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

                var selectedTab by remember { mutableStateOf("HOME") }
                var selectedContentLabel by remember { mutableStateOf("") }
                var showCreateAccount by remember { mutableStateOf(false) }
                var activeChatRoom by remember { mutableStateOf<ChatRoom?>(null) }
                var showNewChat by remember { mutableStateOf(false) }

                val currentLabel = if (selectedContentLabel.isNotBlank())
                    selectedContentLabel else selectedTab

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
                            RetroDrawerContent(
                                selectedContentLabel = selectedContentLabel,
                                onItemSelected = { item ->
                                    SoundManager.playSound(SoundManager.SOUND_NAVIGATION_TAP)
                                    selectedContentLabel = item.label
                                    selectedTab = ""
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }
                    ) {
                        Scaffold(
                            containerColor = Color.Transparent,
                            topBar = {
                                val hidingTopBar = selectedTab == "MESSAGES"
                                        && activeChatRoom != null
                                        && selectedContentLabel.isBlank()
                                if (!hidingTopBar) {
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
                                val screenKey = if (selectedContentLabel.isNotBlank())
                                    selectedContentLabel else selectedTab

                                AnimatedContent(
                                    targetState = screenKey,
                                    modifier = Modifier.fillMaxSize(),
                                    transitionSpec = {
                                        val duration = 600
                                        ContentTransform(
                                            targetContentEnter = scaleIn(
                                                tween(duration - 100, 50, LinearOutSlowInEasing), 0.3f
                                            ) + fadeIn(tween(duration, easing = LinearEasing)),
                                            initialContentExit = scaleOut(
                                                tween(duration - 100, easing = FastOutLinearInEasing), 0.3f
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
                                                },
                                                onNavigateToStreams = {
                                                    selectedContentLabel = "STREAMS"
                                                    selectedTab = ""
                                                },
                                                onNavigateToDiscover = {
                                                    selectedTab = "DISCOVER"
                                                    selectedContentLabel = ""
                                                },
                                                authViewModel = authViewModel
                                            )
                                            "DISCOVER" -> DiscoverScreen(
                                                authViewModel = authViewModel,
                                                chatViewModel = chatViewModel,
                                                streamsViewModel = streamsViewModel,
                                                onNavigateToAlbums = {
                                                    selectedContentLabel = "ALBUMS"
                                                    selectedTab = ""
                                                },
                                                onNavigateToMagazines = {
                                                    selectedContentLabel = "MAGAZINES"
                                                    selectedTab = ""
                                                },
                                                onNavigateToArticles = {
                                                    selectedContentLabel = "ARTICLES"
                                                    selectedTab = ""
                                                },
                                                onNavigateToStreams = {
                                                    selectedContentLabel = "STREAMS"
                                                    selectedTab = ""
                                                },
                                                onNavigateToGameDatabase = {
                                                    selectedContentLabel = "GAMES"
                                                    selectedTab = ""
                                                }
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
                                                        onOpenChat = { room -> activeChatRoom = room },
                                                        onNewChat = { showNewChat = true }
                                                    )
                                                }
                                            }
                                            "MAGAZINES" -> MagazinesScreen(favoritesViewModel = favoritesViewModel)
                                            "ALBUMS" -> AlbumsScreen(favoritesViewModel = favoritesViewModel)
                                            "ARTICLES" -> ArticlesScreen(
                                                favoritesViewModel = favoritesViewModel,
                                                authViewModel = authViewModel,
                                                activityViewModel = activityViewModel,
                                                userArticlesViewModel = userArticlesViewModel
                                            )
                                            "STREAMS" -> StreamsScreen(
                                                streamsViewModel = streamsViewModel,
                                                authViewModel = authViewModel
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
                                            "GAMES" -> GameDatabaseScreen()
                                            "EVENTS" -> EventsScreen(authViewModel = authViewModel)
                                            "MARKETPLACE" -> MarketplaceScreen(
                                                authViewModel = authViewModel,
                                                chatViewModel = chatViewModel
                                            )
                                            else -> HomeScreen(
                                                onNavigateToAlbums = {
                                                    selectedContentLabel = "ALBUMS"
                                                    selectedTab = ""
                                                },
                                                onNavigateToMagazines = {
                                                    selectedContentLabel = "MAGAZINES"
                                                    selectedTab = ""
                                                },
                                                onNavigateToArticles = {
                                                    selectedContentLabel = "ARTICLES"
                                                    selectedTab = ""
                                                },
                                                onNavigateToProfile = {
                                                    selectedTab = "PROFILE"
                                                    selectedContentLabel = ""
                                                },
                                                onNavigateToStreams = {
                                                    selectedContentLabel = "STREAMS"
                                                    selectedTab = ""
                                                },
                                                onNavigateToDiscover = {
                                                    selectedTab = "DISCOVER"
                                                    selectedContentLabel = ""
                                                },
                                                authViewModel = authViewModel
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Robot + Radio
                    if (activeChatRoom == null && !showNewChat) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(bottom = 64.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                val firebaseProfile by authViewModel.userProfile.collectAsState()

                                TalkingRobot(
                                    message = robotMessage,
                                    isVisible = robotVisible,
                                    robotSpriteResId = R.drawable.robot,
                                    habboUsername = firebaseProfile?.habboUsername ?: "",
                                    habboRegion = firebaseProfile?.habboRegion?.ifBlank { "habbo.com" } ?: "habbo.com",
                                    showHabboAvatar = firebaseProfile?.habboUsername?.isNotBlank() == true,
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

// ─── Drawer Content ───────────────────────────────────────────────────────────

@Composable
fun RetroDrawerContent(
    selectedContentLabel: String,
    onItemSelected: (TopActionItem) -> Unit
) {
    ModalDrawerSheet(drawerContainerColor = ScrapbookDark) {

        // ✅ Drawer header with gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(ScrapbookYellow, ScrapbookYellow.copy(alpha = 0.85f))
                    )
                )
                .border(BorderStroke(2.dp, ScrapbookBorder))
                .padding(vertical = 28.dp, horizontal = 20.dp)
        ) {
            Column {
                Text(
                    text = "🕹️",
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "CONTENT",
                    fontFamily = BangersFontFamily,
                    fontSize = 38.sp,
                    color = ScrapbookDark,
                    letterSpacing = 3.sp
                )
                Text(
                    text = "Explore RetroHub",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = ScrapbookDark.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ✅ Drawer items
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            drawerNavItems.forEach { item ->
                val isSelected = item.label == selectedContentLabel
                val icon = drawerNavIcons[item.label]

                // ✅ Press scale animation
                var pressed by remember { mutableStateOf(false) }
                val itemScale by animateFloatAsState(
                    targetValue = if (pressed) 0.96f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "drawerItem_$item"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(itemScale)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected)
                                Brush.horizontalGradient(
                                    colors = listOf(ScrapbookYellow, ScrapbookYellow.copy(alpha = 0.8f))
                                )
                            else
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.05f),
                                        Color.White.copy(alpha = 0.02f)
                                    )
                                )
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) ScrapbookBorder else Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            pressed = true
                            onItemSelected(item)
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Icon circle
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) ScrapbookDark
                                    else Color.White.copy(alpha = 0.1f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (icon != null) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = item.label,
                                    tint = if (isSelected) ScrapbookYellow else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = item.label,
                            fontFamily = BangersFontFamily,
                            fontSize = 22.sp,
                            letterSpacing = 1.sp,
                            color = if (isSelected) ScrapbookDark else Color.White.copy(alpha = 0.85f)
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(ScrapbookDark)
                            )
                        }
                    }
                }

                LaunchedEffect(pressed) {
                    if (pressed) { delay(150); pressed = false }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

// ─── Bottom Nav ───────────────────────────────────────────────────────────────

@Composable
fun ScrapbookBottomNav(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    totalUnread: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScrapbookDark)
            .border(BorderStroke(2.dp, ScrapbookYellow.copy(alpha = 0.4f)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = selectedTab == item.label

                // ✅ Press scale
                var pressed by remember { mutableStateOf(false) }
                val itemScale by animateFloatAsState(
                    targetValue = if (pressed) 0.85f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "nav_scale_${item.label}"
                )

                // ✅ Selected indicator glow
                val glowT = rememberInfiniteTransition(label = "glow_${item.label}")
                val glowAlpha by glowT.animateFloat(
                    initialValue = 0.5f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(1000, easing = EaseInOut), RepeatMode.Reverse
                    ),
                    label = "glowAlpha_${item.label}"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .scale(itemScale)
                        .clickable {
                            pressed = true
                            onTabSelected(item.label)
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        // ✅ Icon with yellow pill background when selected
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) ScrapbookYellow.copy(alpha = glowAlpha)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (isSelected) ScrapbookDark else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(if (isSelected) 26.dp else 22.dp)
                                )
                                // Unread badge
                                if (item.label == "MESSAGES" && totalUnread > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 6.dp, y = (-4).dp)
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(ScrapbookYellow)
                                            .border(1.5.dp, ScrapbookDark, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (totalUnread > 9) "9+" else "$totalUnread",
                                            fontFamily = BangersFontFamily,
                                            color = ScrapbookDark,
                                            fontSize = 8.sp
                                        )
                                    }
                                }
                            }
                        }
                        // Label
                        Text(
                            text = item.label,
                            fontFamily = BangersFontFamily,
                            color = if (isSelected) ScrapbookYellow else Color.White.copy(alpha = 0.4f),
                            fontSize = if (isSelected) 11.sp else 10.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                LaunchedEffect(pressed) {
                    if (pressed) { delay(150); pressed = false }
                }
            }
        }
    }
}

// ─── Top Bar ──────────────────────────────────────────────────────────────────

@Composable
fun RetroAppBar(
    currentScreenLabel: String,
    onNavigationIconClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ✅ Shimmer on title
    val shimmerT = rememberInfiniteTransition(label = "topBarShimmer")
    val shimmerX by shimmerT.animateFloat(
        initialValue = -300f, targetValue = 800f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX"
    )
    val shimmerStartX: Float = shimmerX - 150f
    val shimmerEndX: Float = shimmerX + 150f

    // ✅ Menu button pulse
    val menuT = rememberInfiniteTransition(label = "menuPulse")
    val menuScale by menuT.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse),
        label = "menuScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(ScrapbookDark)
            .border(BorderStroke(2.dp, ScrapbookYellow.copy(alpha = 0.3f)))
    ) {
        // ✅ Subtle top yellow accent line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            ScrapbookYellow.copy(alpha = 0.8f),
                            ScrapbookYellow,
                            ScrapbookYellow.copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✅ Menu button with yellow circle + scale pulse
            Box(
                modifier = Modifier
                    .scale(menuScale)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ScrapbookYellow)
                    .border(2.dp, ScrapbookBorder, CircleShape)
                    .clickable { onNavigationIconClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Open Navigation Menu",
                    tint = ScrapbookDark,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // ✅ Title with shimmer sweep
            Box(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentScreenLabel.uppercase(),
                    color = Color.White,
                    fontFamily = BangersFontFamily,
                    fontSize = 26.sp,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Start
                )
                // Shimmer overlay
                Text(
                    text = currentScreenLabel.uppercase(),
                    fontFamily = BangersFontFamily,
                    fontSize = 26.sp,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Start,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                ScrapbookYellow.copy(alpha = 0.6f),
                                Color.Transparent
                            ),
                            start = androidx.compose.ui.geometry.Offset(shimmerStartX, 0f),
                            end = androidx.compose.ui.geometry.Offset(shimmerEndX, 0f)
                        )
                    )
                )
            }

            // ✅ Right side — current screen pill badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ScrapbookYellow.copy(alpha = 0.15f))
                    .border(1.dp, ScrapbookYellow.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "RETROHUB",
                    fontFamily = BangersFontFamily,
                    color = ScrapbookYellow.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
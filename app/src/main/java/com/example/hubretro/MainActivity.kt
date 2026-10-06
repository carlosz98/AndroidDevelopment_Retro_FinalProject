package com.example.hubretro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import com.example.hubretro.utils.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Data Classes ─────────────────────────────────────────────────────────────

data class TopActionItem(val label: String, val route: String)
data class BottomNavItem(val label: String, val icon: ImageVector)

data class DrawerSection(val title: String, val items: List<TopActionItem>)

data class DrawerItemMeta(
    val icon: ImageVector,
    val accentColor: Color,
    val emoji: String
)

data class ScreenConfig(
    val icon: ImageVector,
    val accentColor: Color,
    val subtitle: String
)

// ─── Screen Configs ───────────────────────────────────────────────────────────

val screenConfigs = mapOf(
    "HOME"         to ScreenConfig(Icons.Filled.Home,           CAcYellow, "Welcome back"),
    "DISCOVER"     to ScreenConfig(Icons.Filled.Explore,        CGreen, "Find something new"),
    "MESSAGES"     to ScreenConfig(Icons.Filled.Chat,           CAcBlue, "Your conversations"),
    "PROFILE"      to ScreenConfig(Icons.Filled.Person,         CAcPurple, "Your retro identity"),
    "CHECKPOINTS"  to ScreenConfig(Icons.Filled.Flag,           CGreen,   "milestones & moments"),
    "MAGAZINES"    to ScreenConfig(Icons.Filled.MenuBook,       CAcYellow, "Latest issues"),
    "ALBUMS"       to ScreenConfig(Icons.Filled.Album,          CAcRed, "Game soundtracks"),
    "ARTICLES"     to ScreenConfig(Icons.Filled.Article,        CAcRed, "News & stories"),
    "STREAMS"      to ScreenConfig(Icons.Filled.LiveTv,         CAcPurple, "Live now"),
    "GAMES"        to ScreenConfig(Icons.Filled.SportsEsports,  CGreenDeep, "Game database"),
    "EVENTS"       to ScreenConfig(Icons.Filled.Event,          CGreen, "Upcoming events"),
    "MARKETPLACE"  to ScreenConfig(Icons.Filled.Store,          ScrapbookTextMuted, "Buy & sell retro"),
    "RETROBYTES"   to ScreenConfig(Icons.Filled.RssFeed,        CAcRed, "Retro bytes feed"),
    "SUPPORT"      to ScreenConfig(Icons.Filled.SupportAgent,   CGreen, "Help desk")
)

// ─── Drawer Data ──────────────────────────────────────────────────────────────

val drawerSections = listOf(
    DrawerSection("MEDIA", listOf(
        TopActionItem("MAGAZINES", "magazines"),
        TopActionItem("ALBUMS",    "albums"),
        TopActionItem("ARTICLES",  "articles")
    )),
    DrawerSection("COMMUNITY", listOf(
        TopActionItem("STREAMS",  "streams"),
        TopActionItem("EVENTS",   "events"),
        TopActionItem("MESSAGES", "messages")
    )),
    DrawerSection("EXPLORE", listOf(
        TopActionItem("GAMES",       "games"),
        TopActionItem("MARKETPLACE", "marketplace")
    )),
    DrawerSection("HELP", listOf(
        TopActionItem("SUPPORT", "support")
    ))
)

val drawerItemMeta = mapOf(
    "MAGAZINES"   to DrawerItemMeta(Icons.Filled.MenuBook,       CAcYellow, "📖"),
    "ALBUMS"      to DrawerItemMeta(Icons.Filled.Album,          CAcRed, "🎵"),
    "ARTICLES"    to DrawerItemMeta(Icons.Filled.Article,        CAcRed, "📰"),
    "STREAMS"     to DrawerItemMeta(Icons.Filled.LiveTv,         CAcPurple, "📺"),
    "EVENTS"      to DrawerItemMeta(Icons.Filled.Event,          CGreen, "🎟️"),
    "MESSAGES"    to DrawerItemMeta(Icons.Filled.Chat,           CAcBlue, "💬"),
    "GAMES"       to DrawerItemMeta(Icons.Filled.SportsEsports,  CGreenDeep, "🕹️"),
    "MARKETPLACE" to DrawerItemMeta(Icons.Filled.Store,          ScrapbookTextMuted, "🏪"),
    "SUPPORT"     to DrawerItemMeta(Icons.Filled.SupportAgent,   CGreen, "🛟")
)

val drawerNavItems  = drawerSections.flatMap { it.items }
val drawerNavIcons  = drawerItemMeta.mapValues { it.value.icon }

val bottomNavItems = listOf(
    BottomNavItem("DISCOVER",    Icons.Filled.Explore),
    BottomNavItem("MESSAGES",    Icons.Filled.Chat),
    BottomNavItem("HOME",        Icons.Filled.Home),
    BottomNavItem("CHECKPOINTS", Icons.Filled.Flag),
    BottomNavItem("PROFILE",     Icons.Filled.Person)
)

val drawerTaglines = listOf(
    "YOUR RETRO UNIVERSE",
    "TUNE IN. READ UP. PLAY ON.",
    "NOSTALGIA LIVES HERE",
    "EXPLORE THE CLASSICS",
    "RETRO NEVER DIES"
)

// ─── MainActivity ─────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SoundManager.initialize(applicationContext)
        Chiptune.init(applicationContext)
        RetroNotify.createChannels(applicationContext)
        RetroSync.schedule(applicationContext)
        handleNotificationIntent(intent)          // opened from a notification?
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
          // Launch intro: plays once per fresh launch and sits above every screen
          // (auth gate, onboarding and the main app alike). Tap or back to skip.
          var introDone by rememberSaveable { mutableStateOf(false) }
          Box(Modifier.fillMaxSize()) {
          ProvideGlowClock {
            HubRetroTheme {
                val authViewModel: AuthViewModel             = viewModel()
                val favoritesViewModel: FavoritesViewModel   = viewModel()
                val activityViewModel: ActivityViewModel     = viewModel()
                val userArticlesViewModel: UserArticlesViewModel = viewModel()
                val achievementsViewModel: AchievementsViewModel = viewModel()
                val retroRadioViewModel: RetroRadioViewModel = viewModel()
                val chatViewModel: ChatViewModel             = viewModel()
                val streamsViewModel: StreamsViewModel       = viewModel()
                val nowPlayingViewModel: NowPlayingViewModel = viewModel()
                val postViewModel: PostViewModel             = viewModel()
                val notificationsViewModel: NotificationsViewModel = viewModel()

                val currentUser       by authViewModel.currentUser.collectAsState()
                val totalUnread       by chatViewModel.totalUnread.collectAsState()
                val miniNowPlaying    by nowPlayingViewModel.nowPlaying.collectAsState()
                val miniSelectedTrack by nowPlayingViewModel.selectedTrack.collectAsState()
                val miniIsPlaying     by nowPlayingViewModel.isPlaying.collectAsState()

                favoritesViewModel.activityViewModel     = activityViewModel
                authViewModel.activityViewModel          = activityViewModel
                activityViewModel.achievementsViewModel  = achievementsViewModel

                LaunchedEffect(currentUser?.uid) {
                    if (currentUser != null) {
                        achievementsViewModel.refreshForUser()
                        chatViewModel.listenToChatRooms()
                        notificationsViewModel.fetchNotifications()
                    }
                }

                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                // Let page headers open the drawer (menu button in ComicPageHeader)
                SideEffect { DrawerController.open = { scope.launch { drawerState.open() } } }

                var selectedTab          by remember { mutableStateOf("HOME") }
                var selectedContentLabel by remember { mutableStateOf("") }
                var showCreateAccount    by remember { mutableStateOf(false) }
                var activeChatRoom       by remember { mutableStateOf<ChatRoom?>(null) }
                var showNewChat          by remember { mutableStateOf(false) }
                var showNotifications    by remember { mutableStateOf(false) }
                var taglineIndex         by remember { mutableStateOf(0) }

                LaunchedEffect(drawerState.currentValue) {
                    if (drawerState.currentValue == DrawerValue.Open) {
                        taglineIndex = (taglineIndex + 1) % drawerTaglines.size
                    }
                }

                val currentLabel = if (selectedContentLabel.isNotBlank()) selectedContentLabel else selectedTab

                LaunchedEffect(Unit) {
                    RobotBrain.init(applicationContext)
                    RobotBrain.startSession()
                }
                LaunchedEffect(currentLabel) {
                    RobotBrain.notify(RobotTrigger.ScreenEnter(currentLabel.lowercase()))
                }

                // ── Auth gate: nothing else is reachable until the account exists + is set up ──
                val gateProfile by authViewModel.userProfile.collectAsState()
                val profileLoaded by authViewModel.profileLoaded.collectAsState()
                // Hyperspace "level load" overlay (account created / welcome back / entering RetroHub)
                WarpTransitionHost()
                if (currentUser == null) {
                    AuthGate(
                        authViewModel = authViewModel,
                        showCreateAccount = showCreateAccount,
                        onShowCreateAccount = { showCreateAccount = it }
                    )
                    return@HubRetroTheme
                }
                // ── Moderation: live strike/ban sync + warning dialogs ──
                ModerationWatcher(uid = currentUser?.uid)
                ModerationNoticeHost()
                if (Moderation.banned) {
                    BannedScreen(reason = Moderation.bannedReason, onSignOut = { authViewModel.signOut() })
                    return@HubRetroTheme
                }
                // Signed in but profile not loaded yet → loading warp (never flash the main page)
                if (!profileLoaded || gateProfile == null) {
                    PlayerLoadingScreen(onRetry = { currentUser?.uid?.let { authViewModel.fetchUserProfile(it) } })
                    return@HubRetroTheme
                }
                if (gateProfile?.setupComplete == false) {
                    OnboardingGate(authViewModel = authViewModel)
                    return@HubRetroTheme
                }

                // ── Notifications: FCM token/topics, permission prompt, weekly update + settings sheets ──
                val notifContext = LocalContext.current
                LaunchedEffect(currentUser?.uid) { if (currentUser != null) RetroPush.register(notifContext) }
                NotificationPermissionAsker()
                WeeklyDigestHost()
                NotificationSettingsHost()

                // ── Notifications overlay ────────────────────────────────────
                if (showNotifications) {
                    NotificationsScreen(
                        onBack = { showNotifications = false },
                        onNavigateToProfile = { _ ->
                            showNotifications = false
                            selectedTab = "DISCOVER"
                            selectedContentLabel = ""
                        },
                        notificationsViewModel = notificationsViewModel
                    )
                    return@HubRetroTheme
                }

                val isOnAlbums = selectedContentLabel == "ALBUMS"
                val unreadNotifCount by remember { derivedStateOf { notificationsViewModel.unreadCount } }

                // Deep links requested by any screen (e.g. Game Database → Albums / Marketplace)
                val navRequest = AppNavBus.request
                LaunchedEffect(navRequest) {
                    val target = navRequest ?: return@LaunchedEffect
                    AppNavBus.request = null
                    if (bottomNavItems.any { it.label == target }) {
                        selectedTab = target; selectedContentLabel = ""
                    } else {
                        selectedContentLabel = target; selectedTab = ""
                    }
                }

                // Shake the phone → random game surprise
                ShakeDetector {
                    Chiptune.play(Chiptune.Sfx.COIN)
                    TvStaticBus.play {
                        SurpriseBus.pending = true
                        selectedContentLabel = "GAMES"; selectedTab = ""
                    }
                }

                Box(modifier = Modifier.fillMaxSize().background(ComicGlassBg).inkSplatTaps()) {
                    // Global overlays (zIndex keeps them above the app)
                    LevelUpWatcher(achievementsViewModel)
                    AchievementWatcher(achievementsViewModel)
                    PixelDustLayer(Modifier.zIndex(4f))
                    TvStaticOverlay()
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            RetroDrawerContent(
                                selectedContentLabel = selectedContentLabel,
                                tagline              = drawerTaglines[taglineIndex],
                                nowPlaying           = miniNowPlaying,
                                selectedTrack        = miniSelectedTrack,
                                isPlaying            = miniIsPlaying,
                                onItemSelected = { item ->
                                    SoundManager.playSound(SoundManager.SOUND_NAVIGATION_TAP)
                                    // Bottom-bar pages (e.g. MESSAGES) open as their tab so the nav highlights correctly
                                    if (bottomNavItems.any { it.label == item.label }) {
                                        selectedTab = item.label; selectedContentLabel = ""
                                    } else {
                                        selectedContentLabel = item.label; selectedTab = ""
                                    }
                                    scope.launch { drawerState.close() }
                                },
                                onNowPlayingClick = {
                                    selectedContentLabel = "ALBUMS"
                                    selectedTab = ""
                                    scope.launch { drawerState.close() }
                                },
                                authViewModel = authViewModel
                            )
                        }
                    ) {
                        Scaffold(
                            containerColor = Color.Transparent,
                            topBar = {
                                // These pages draw their own header — never stack a second one on top
                                val shownLabel = (if (selectedContentLabel.isNotBlank()) selectedContentLabel else selectedTab).uppercase()
                                val hidingTopBar = shownLabel in setOf("MESSAGES", "CHECKPOINTS", "DISCOVER", "ARTICLES")
                                if (!hidingTopBar) {
                                    val pageLabel = (if (selectedContentLabel.isNotBlank()) selectedContentLabel else selectedTab).uppercase()
                                    if (pageLabel == "PROFILE") {
                                    Box(modifier = Modifier.padding(top = 40.dp)) {
                                        RetroAppBar(
                                            currentScreenLabel  = if (selectedContentLabel.isNotBlank()) selectedContentLabel else selectedTab,
                                            onNavigationIconClick = {
                                                SoundManager.playSound(SoundManager.SOUND_NAVIGATION_TAP)
                                                scope.launch {
                                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                                }
                                            },
                                            isPlayingMusic  = miniNowPlaying != null,
                                            nowPlaying      = miniNowPlaying,
                                            selectedTrack   = miniSelectedTrack,
                                            onNowPlayingClick = {
                                                selectedContentLabel = "ALBUMS"; selectedTab = ""
                                            },
                                            unreadNotifCount = unreadNotifCount,
                                            onNotificationsTap = { showNotifications = true },
                                            authViewModel   = authViewModel,
                                            onNavigateToProfile = {
                                                selectedTab = "PROFILE"; selectedContentLabel = ""
                                            }
                                        )
                                    }
                                    } else {
                                        // Every other page: big green comic header + scrolling marquee strip
                                        Box(modifier = Modifier.statusBarsPadding()) {
                                            ComicPageHeader(
                                                title = pageTitleFor(pageLabel),
                                                subtitle = pageSubtitleFor(pageLabel),
                                                marquee = pageMarqueeFor(pageLabel)
                                            ) {
                                                Box {
                                                    ComicIconButton(Icons.Filled.Notifications, "Notifications") { showNotifications = true }
                                                    if (unreadNotifCount > 0) {
                                                        Box(
                                                            modifier = Modifier.align(Alignment.TopEnd).size(17.dp).clip(CircleShape)
                                                                .background(CAcRed).border(1.5.dp, ScrapbookDark, CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(if (unreadNotifCount > 9) "9+" else "$unreadNotifCount",
                                                                fontFamily = BangersFontFamily, color = Color.White, fontSize = 8.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            bottomBar = {
                                ScrapbookBottomNav(
                                    selectedTab          = selectedTab,
                                    selectedContentLabel = selectedContentLabel,
                                    onTabSelected = { tab ->
                                        SoundManager.playSound(SoundManager.SOUND_NAVIGATION_TAP)
                                        selectedTab = tab
                                        selectedContentLabel = ""
                                        activeChatRoom = null
                                        showNewChat = false
                                    },
                                    totalUnread    = totalUnread,
                                    isPlayingMusic = miniNowPlaying != null
                                )
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier.padding(innerPadding).fillMaxSize().background(ComicGlassBg)
                            ) {
                                val screenKey = if (selectedContentLabel.isNotBlank()) selectedContentLabel else selectedTab

                                AnimatedContent(
                                    targetState = screenKey,
                                    modifier    = Modifier.fillMaxSize(),
                                    // Comic-panel transition: the new screen slashes in on a diagonal ink cut
                                    // (animation lives in Modifier.comicPanelTransition below)
                                    transitionSpec = {
                                        ContentTransform(
                                            targetContentEnter = EnterTransition.None,
                                            initialContentExit = ExitTransition.None,
                                            targetContentZIndex = 1f
                                        )
                                    },
                                    label = "ScreenTransition"
                                ) { target ->
                                    Box(modifier = Modifier.fillMaxSize().comicPanelTransition(this@AnimatedContent).background(ComicGlassBg)) {
                                        when (target.uppercase()) {

                                            "HOME" -> HomeScreen(
                                                onNavigateToAlbums      = { SoundManager.playSound(SoundManager.SOUND_BUTTON_PRIMARY_CLICK); selectedContentLabel = "ALBUMS";       selectedTab = "" },
                                                onNavigateToMagazines   = { SoundManager.playSound(SoundManager.SOUND_BUTTON_PRIMARY_CLICK); selectedContentLabel = "MAGAZINES";   selectedTab = "" },
                                                onNavigateToArticles    = { SoundManager.playSound(SoundManager.SOUND_BUTTON_PRIMARY_CLICK); selectedContentLabel = "ARTICLES";    selectedTab = "" },
                                                onNavigateToProfile     = { SoundManager.playSound(SoundManager.SOUND_BUTTON_PRIMARY_CLICK); selectedTab = "PROFILE"; selectedContentLabel = "" },
                                                onNavigateToStreams      = { selectedContentLabel = "STREAMS";      selectedTab = "" },
                                                onNavigateToDiscover    = { selectedTab = "DISCOVER";              selectedContentLabel = "" },
                                                onNavigateToGames       = { selectedContentLabel = "GAMES";        selectedTab = "" },
                                                onNavigateToRetroBytes  = { selectedContentLabel = "RETROBYTES";   selectedTab = "" },
                                                onNavigateToEvents      = { selectedContentLabel = "EVENTS";       selectedTab = "" },
                                                onNavigateToMarketplace = { selectedContentLabel = "MARKETPLACE";  selectedTab = "" },
                                                onNavigateToCheckpoints = { selectedContentLabel = "CHECKPOINTS";  selectedTab = "" },
                                                authViewModel           = authViewModel,
                                                postViewModel           = postViewModel
                                            )

                                            "CHECKPOINTS" -> CheckpointScreen(
                                                onBack        = { selectedContentLabel = ""; selectedTab = "HOME" },
                                                authViewModel = authViewModel
                                            )

                                            "DISCOVER" -> DiscoverScreen(
                                                authViewModel         = authViewModel,
                                                chatViewModel         = chatViewModel,
                                                streamsViewModel      = streamsViewModel,
                                                onNavigateToAlbums    = { selectedContentLabel = "ALBUMS";    selectedTab = "" },
                                                onNavigateToMagazines = { selectedContentLabel = "MAGAZINES"; selectedTab = "" },
                                                onNavigateToArticles  = { selectedContentLabel = "ARTICLES";  selectedTab = "" },
                                                onNavigateToStreams    = { selectedContentLabel = "STREAMS";   selectedTab = "" },
                                                onNavigateToGameDatabase = { selectedContentLabel = "GAMES";  selectedTab = "" },
                                                onNavigateToEvents     = { selectedContentLabel = "EVENTS";      selectedTab = "" },
                                                onNavigateToMarketplace = { selectedContentLabel = "MARKETPLACE"; selectedTab = "" },
                                                onNavigateToCheckpoints = { selectedContentLabel = "CHECKPOINTS"; selectedTab = "" },
                                                onNavigateToRetroBytes = { selectedContentLabel = "RETROBYTES";  selectedTab = "" },
                                                achievementsViewModel  = achievementsViewModel
                                            )

                                            "MESSAGES" -> when {
                                                activeChatRoom != null -> ChatScreen(
                                                    chatRoom      = activeChatRoom!!,
                                                    chatViewModel = chatViewModel,
                                                    authViewModel = authViewModel,
                                                    onBack        = { activeChatRoom = null }
                                                )
                                                showNewChat -> NewChatScreen(
                                                    chatViewModel = chatViewModel,
                                                    authViewModel = authViewModel,
                                                    onChatCreated = { chatId ->
                                                        showNewChat = false
                                                        val state = chatViewModel.chatRooms.value
                                                        if (state is ChatUiState.Success)
                                                            activeChatRoom = state.rooms.firstOrNull { it.id == chatId }
                                                    },
                                                    onBack = { showNewChat = false }
                                                )
                                                else -> ChatListScreen(
                                                    chatViewModel = chatViewModel,
                                                    authViewModel = authViewModel,
                                                    onOpenChat    = { room -> activeChatRoom = room },
                                                    onNewChat     = { showNewChat = true }
                                                )
                                            }

                                            "MAGAZINES"   -> MagazinesScreen(favoritesViewModel = favoritesViewModel)
                                            "ALBUMS"      -> AlbumsScreen(favoritesViewModel = favoritesViewModel, nowPlayingViewModel = nowPlayingViewModel)
                                            "ARTICLES"    -> ArticlesScreen(favoritesViewModel = favoritesViewModel, authViewModel = authViewModel)
                                            "STREAMS"     -> StreamsScreen(streamsViewModel = streamsViewModel)

                                            "PROFILE" -> {
                                                if (currentUser != null) {
                                                    val profile by authViewModel.userProfile.collectAsState()
                                                    if (profile?.setupComplete == true) {
                                                        ProfileScreen(authViewModel = authViewModel, favoritesViewModel = favoritesViewModel, activityViewModel = activityViewModel, achievementsViewModel = achievementsViewModel)
                                                    } else {
                                                        ProfileSetupScreen(authViewModel = authViewModel, onSetupComplete = { })
                                                    }
                                                } else if (showCreateAccount) {
                                                    CreateAccountScreen(authViewModel = authViewModel, onAccountCreated = { showCreateAccount = false }, onNavigateToLogin = { showCreateAccount = false })
                                                } else {
                                                    LoginScreen(authViewModel = authViewModel, onLoginSuccess = { }, onNavigateToCreateAccount = { showCreateAccount = true })
                                                }
                                            }

                                            "GAMES"       -> GameDatabaseScreen()
                                            "EVENTS"      -> EventsScreen(authViewModel = authViewModel)
                                            "MARKETPLACE" -> MarketplaceScreen(authViewModel = authViewModel, chatViewModel = chatViewModel)
                                            "RETROBYTES"  -> RetroBytesScreen()
                                            "SUPPORT"     -> SupportScreen(authViewModel = authViewModel)

                                            else -> HomeScreen(
                                                onNavigateToAlbums      = { selectedContentLabel = "ALBUMS";      selectedTab = "" },
                                                onNavigateToMagazines   = { selectedContentLabel = "MAGAZINES";   selectedTab = "" },
                                                onNavigateToArticles    = { selectedContentLabel = "ARTICLES";    selectedTab = "" },
                                                onNavigateToProfile     = { selectedTab = "PROFILE";              selectedContentLabel = "" },
                                                onNavigateToStreams      = { selectedContentLabel = "STREAMS";     selectedTab = "" },
                                                onNavigateToDiscover    = { selectedTab = "DISCOVER";             selectedContentLabel = "" },
                                                onNavigateToGames       = { selectedContentLabel = "GAMES";       selectedTab = "" },
                                                onNavigateToRetroBytes  = { selectedContentLabel = "RETROBYTES";  selectedTab = "" },
                                                onNavigateToEvents      = { selectedContentLabel = "EVENTS";      selectedTab = "" },
                                                onNavigateToMarketplace = { selectedContentLabel = "MARKETPLACE"; selectedTab = "" },
                                                authViewModel           = authViewModel,
                                                postViewModel           = postViewModel
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Mini Now Playing Player ────────────────────────────────
                    AnimatedVisibility(
                        visible  = miniNowPlaying != null && !isOnAlbums,
                        enter    = slideInVertically(tween(400, easing = LinearOutSlowInEasing)) { it } + fadeIn(tween(300)),
                        exit     = slideOutVertically(tween(300, easing = FastOutLinearInEasing)) { it } + fadeOut(tween(200)),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 72.dp).zIndex(20f)
                    ) {
                        MiniNowPlayingPlayer(
                            nowPlayingViewModel = nowPlayingViewModel,
                            onExpand = { selectedContentLabel = "ALBUMS"; selectedTab = "" }
                        )
                    }

                    // ── Robot + Radio ──────────────────────────────────────────
                    if (activeChatRoom == null && !showNewChat) {
                        Box(
                            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                                .padding(bottom = if (miniNowPlaying != null && !isOnAlbums) 130.dp else 64.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                val firebaseProfile by authViewModel.userProfile.collectAsState()
                                RobotHost(
                                    habboUsername = firebaseProfile?.habboUsername ?: "",
                                    habboRegion   = firebaseProfile?.habboRegion?.ifBlank { "habbo.com" } ?: "habbo.com"
                                )
                                RetroRadioPlayer(radioViewModel = retroRadioViewModel, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
          } // ProvideGlowClock
          if (!introDone) {
              RetroHubIntro(
                  onFinished = { introDone = true },
                  onImpact = { Chiptune.play(Chiptune.Sfx.STAMP) }
              )
          }
          } // intro Box
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)          // notification tapped while the app is open
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundManager.release()
    }
}

// ─── Drawer ───────────────────────────────────────────────────────────────────

@Composable
fun RetroDrawerContent(
    selectedContentLabel: String,
    tagline: String,
    nowPlaying: NowPlayingState?,
    selectedTrack: AlbumTrack?,
    isPlaying: Boolean,
    onItemSelected: (TopActionItem) -> Unit,
    onNowPlayingClick: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val neonAlpha by rememberGlowRange(0.4f, 1f)

    ModalDrawerSheet(drawerContainerColor = ComicGlassBg) {

        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(CGreen, CGreen.copy(alpha = 0.85f))))
                .border(BorderStroke(2.dp, ScrapbookBorder))
                .padding(vertical = 24.dp, horizontal = 20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape)
                            .background(ScrapbookDark).border(2.dp, ScrapbookBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text("🕹️", fontSize = 20.sp) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("CONTENT", fontFamily = BangersFontFamily, fontSize = 32.sp, color = ScrapbookDark, letterSpacing = 3.sp)
                        Text(tagline, fontFamily = NunitoFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, color = ScrapbookDark.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    // 8-bit sound effects on/off
                    val sfxContext = LocalContext.current
                    ComicIconButton(
                        if (Chiptune.isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                        "Sound effects"
                    ) { Chiptune.toggleMute(sfxContext) }
                    // Notification settings
                    ComicIconButton(Icons.Filled.NotificationsActive, "Notification settings") {
                        NotificationSettingsBus.open = true
                    }
                }

                // Now-playing strip
                AnimatedVisibility(visible = nowPlaying != null) {
                    if (nowPlaying != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ScrapbookDark.copy(alpha = 0.15f))
                                .border(1.dp, ScrapbookDark.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable { onNowPlayingClick() }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Spinning vinyl disc
                                val spinT = rememberInfiniteTransition(label = "drawerNpSpin")
                                val spinAngle by spinT.animateFloat(
                                    0f, 360f,
                                    infiniteRepeatable(tween(if (isPlaying) 3000 else 9000, easing = LinearEasing), RepeatMode.Restart),
                                    label = "drawerNpSpinAngle"
                                )
                                Box(
                                    modifier = Modifier.size(28.dp).clip(CircleShape)
                                        .background(ScrapbookDark).border(1.dp, ScrapbookBorder, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(modifier = Modifier.size(28.dp).clip(CircleShape).rotate(spinAngle), contentAlignment = Alignment.Center) {
                                        Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(CGreenDeep))
                                        when {
                                            nowPlaying.coverResId != null -> Image(painter = painterResource(id = nowPlaying.coverResId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(18.dp).clip(CircleShape), alpha = 0.9f)
                                            nowPlaying.coverUrl  != null -> AsyncImage(model = nowPlaying.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(nowPlaying.coverUrl).size(18.dp).clip(CircleShape), alpha = 0.9f)
                                        }
                                    }
                                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(ScrapbookDark))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    if (isPlaying) {
                                        val dotT = rememberInfiniteTransition(label = "drawerDot")
                                        val dotA by dotT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "drawerDotA")
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(ScrapbookDark.copy(alpha = dotA)))
                                            Text("NOW PLAYING", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 8.sp, letterSpacing = 1.sp)
                                        }
                                    }
                                    Text(selectedTrack?.title ?: nowPlaying.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Icon(Icons.Filled.KeyboardArrowRight, null, tint = ScrapbookDark.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Items ─────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            drawerSections.forEachIndexed { sectionIndex, section ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.width(3.dp).height(10.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(alpha = neonAlpha * 0.8f)))
                    Text(section.title, fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.55f), fontSize = 10.sp, letterSpacing = 2.sp)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = ScrapbookDark.copy(alpha = 0.2f), thickness = 1.dp)
                }

                section.items.forEach { item ->
                    val isSelected = item.label == selectedContentLabel
                    val meta = drawerItemMeta[item.label]
                    var pressed by remember { mutableStateOf(false) }
                    val itemScale by animateFloatAsState(
                        targetValue = if (pressed) 0.96f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "drawerItem_${item.label}"
                    )

                    Box(modifier = Modifier.fillMaxWidth().scale(itemScale)) {
                        if (isSelected) {
                            Box(modifier = Modifier.matchParentSize().padding(2.dp).blur(8.dp).background(CGreen.copy(alpha = 0.2f), RoundedCornerShape(12.dp)))
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) Brush.horizontalGradient(listOf(CGreen, CGreen.copy(alpha = 0.8f)))
                                    else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.46f), Color.White.copy(alpha = 0.46f)))
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) ScrapbookBorder else ScrapbookDark.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { pressed = true; onItemSelected(item) }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape)
                                        .background(if (isSelected) ScrapbookDark else CGreen.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (meta != null) Icon(meta.icon, item.label, tint = if (isSelected) CGreen else ScrapbookDark.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                }
                                Text(item.label, fontFamily = BangersFontFamily, fontSize = 20.sp, letterSpacing = 1.sp, color = if (isSelected) ScrapbookDark else ScrapbookDark.copy(alpha = 0.85f), modifier = Modifier.weight(1f))
                                if (isSelected) Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ScrapbookDark))
                            }
                        }
                    }
                    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
                }

                if (sectionIndex < drawerSections.size - 1) Spacer(modifier = Modifier.height(4.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ─── Bottom Nav ───────────────────────────────────────────────────────────────

@Composable
fun ScrapbookBottomNav(
    selectedTab: String,
    selectedContentLabel: String = "",
    onTabSelected: (String) -> Unit,
    totalUnread: Int,
    isPlayingMusic: Boolean = false
) {
    val tabCount = bottomNavItems.size
    val selectedIndex = bottomNavItems.indexOfFirst { it.label == selectedTab }.coerceAtLeast(0)
    val pillPosition by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "pillSlide"
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        // Scanline texture
        Canvas(modifier = Modifier.fillMaxWidth().height(72.dp)) {
            var y = 0f
            while (y < size.height) {
                drawLine(color = Color.White.copy(alpha = 0.012f),
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end   = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 1f)
                y += 3f
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth()
                .background(ScrapbookDark)
                .border(BorderStroke(2.dp, CGreen.copy(alpha = 0.4f)))
        ) {
            // Top accent line — amber pulse when music playing, yellow glow otherwise
            if (isPlayingMusic) {
                val pulseT = rememberInfiniteTransition(label = "navMusicPulse")
                val pulseAlpha by pulseT.animateFloat(0.4f, 1f,
                    infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse),
                    label = "navMusicPulseAlpha")
                Box(modifier = Modifier.fillMaxWidth().height(2.dp)
                    .background(Brush.horizontalGradient(listOf(
                        Color.Transparent,
                        CGreen.copy(alpha = pulseAlpha),
                        CGreenDeep.copy(alpha = pulseAlpha),
                        CGreen.copy(alpha = pulseAlpha),
                        Color.Transparent
                    ))))
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(2.dp)
                    .background(Brush.horizontalGradient(listOf(
                        Color.Transparent,
                        CGreen.copy(alpha = 0.4f),
                        CGreen.copy(alpha = 0.6f),
                        CGreen.copy(alpha = 0.4f),
                        Color.Transparent
                    ))))
            }

            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                // Sliding pill background
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val itemWidth = maxWidth / tabCount
                    Box(
                        modifier = Modifier
                            .offset(x = itemWidth * pillPosition)
                            .width(itemWidth)
                            // Ink-blob stretch while the pill travels between tabs
                            .graphicsLayer {
                                val travel = kotlin.math.abs(pillPosition - selectedIndex).coerceAtMost(1f)
                                scaleX = 1f + travel * 0.55f
                                scaleY = 1f - travel * 0.18f
                            }
                            .padding(horizontal = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CGreen.copy(alpha = 0.15f))
                            .border(1.dp, CGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .height(52.dp)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    bottomNavItems.forEachIndexed { _, item ->
                        val isSelected = selectedTab == item.label
                        var pressed by remember { mutableStateOf(false) }
                        val itemScale by animateFloatAsState(
                            targetValue = if (pressed) 0.82f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh),
                            label = "nav_scale_${item.label}"
                        )
                        val iconOffsetY by animateFloatAsState(
                            targetValue = if (isSelected) -3f else 0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "iconBounce_${item.label}"
                        )

                        Box(
                            modifier = Modifier.weight(1f).scale(itemScale)
                                .clickable { pressed = true; onTabSelected(item.label) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.offset(y = iconOffsetY.dp)
                            ) {
                                Box(modifier = Modifier.squashOnSelect(isSelected), contentAlignment = Alignment.Center) {
                                    // Spinning vinyl disc on DISCOVER when music plays
                                    if (item.label == "DISCOVER" && isPlayingMusic) {
                                        val vinylT = rememberInfiniteTransition(label = "navVinyl")
                                        val vinylAngle by vinylT.animateFloat(0f, 360f,
                                            infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
                                            label = "navVinylAngle")
                                        Box(
                                            modifier = Modifier.size(26.dp).clip(CircleShape)
                                                .background(ScrapbookDark)
                                                .border(1.dp, CGreen.copy(alpha = 0.7f), CircleShape)
                                                .rotate(vinylAngle),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                val cx = size.width / 2f; val cy = size.height / 2f
                                                for (i in 1..3) {
                                                    drawCircle(color = Color.White.copy(alpha = 0.04f),
                                                        radius = size.width / 2f * (0.3f + i * 0.15f),
                                                        center = androidx.compose.ui.geometry.Offset(cx, cy),
                                                        style  = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.8f))
                                                }
                                                drawCircle(color = CGreenDeep, radius = size.width / 2f * 0.35f, center = androidx.compose.ui.geometry.Offset(cx, cy))
                                                drawCircle(color = Color(0xFF050302), radius = size.width / 2f * 0.08f, center = androidx.compose.ui.geometry.Offset(cx, cy))
                                            }
                                        }
                                    } else {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label,
                                            tint = if (isSelected) CGreen else Color.White.copy(alpha = 0.45f),
                                            modifier = Modifier.size(if (isSelected) 24.dp else 22.dp)
                                        )
                                    }

                                    // Unread badge on MESSAGES
                                    if (item.label == "MESSAGES" && totalUnread > 0) {
                                        Box(
                                            modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-4).dp)
                                                .size(15.dp).clip(CircleShape)
                                                .background(CGreen).border(1.5.dp, ScrapbookDark, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(if (totalUnread > 9) "9+" else "$totalUnread", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 7.sp)
                                        }
                                    }

                                    // Pulsing dot on HOME when music plays
                                    if (isPlayingMusic && item.label == "HOME") {
                                        val dotT = rememberInfiniteTransition(label = "navMusicDot")
                                        val dotA by dotT.animateFloat(0.5f, 1f,
                                            infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "navMusicDotA")
                                        Box(
                                            modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-2).dp)
                                                .size(7.dp).clip(CircleShape)
                                                .background(CGreenDeep.copy(alpha = dotA))
                                                .border(1.dp, ScrapbookDark, CircleShape)
                                        )
                                    }
                                }

                                // Label — animated, only on selected tab
                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter   = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.8f),
                                    exit    = fadeOut(tween(100)) + scaleOut(tween(100), targetScale = 0.8f)
                                ) {
                                    Text(item.label, fontFamily = BangersFontFamily, color = CGreen, fontSize = 9.sp, letterSpacing = 0.5.sp)
                                }
                            }
                        }
                        LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
                    }
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
    modifier: Modifier = Modifier,
    isPlayingMusic: Boolean = false,
    nowPlaying: NowPlayingState? = null,
    selectedTrack: AlbumTrack? = null,
    onNowPlayingClick: () -> Unit = {},
    unreadNotifCount: Int = 0,
    onNotificationsTap: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel(),
    onNavigateToProfile: () -> Unit = {}
) {
    val config      = screenConfigs[currentScreenLabel.uppercase()]
    val accentColor = config?.accentColor ?: CGreen

    val firebaseProfile by authViewModel.userProfile.collectAsState()
    val currentUser     by authViewModel.currentUser.collectAsState()

    val neonAlpha by rememberGlowRange(0.4f, 1f)

    val shimmerT = rememberInfiniteTransition(label = "topBarShimmer")
    val shimmerX by shimmerT.animateFloat(-300f, 800f,
        infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart), label = "shimmerX")

    val menuScale by rememberGlowRange(1f, 1.08f)

    val eqT = rememberInfiniteTransition(label = "appBarEq")
    val eqHeights = (0..2).map { i ->
        eqT.animateFloat(2f, (6 + i * 2).toFloat(),
            infiniteRepeatable(tween(250 + i * 80, easing = EaseInOut), RepeatMode.Reverse),
            label = "appBarEq_$i")
    }

    // Per-page emoji personality
    val pageEmoji = when (currentScreenLabel.uppercase()) {
        "HOME"        -> "★"
        "DISCOVER"    -> "🔍"
        "MESSAGES"    -> "💬"
        "PROFILE"     -> "🎮"
        "CHECKPOINTS" -> "🏁"
        "MAGAZINES"   -> "📰"
        "ALBUMS"      -> "🎵"
        "ARTICLES"    -> "✍️"
        "STREAMS"     -> "🔴"
        "GAMES"       -> "👾"
        "EVENTS"      -> "📅"
        "MARKETPLACE" -> "🛒"
        "RETROBYTES"  -> "📱"
        "SUPPORT"     -> "🛟"
        else          -> "★"
    }

    // Comic Glass AppBar — white glass panel + 2.5dp border + 4dp shadow + animated top stripe
    Box(modifier = modifier.fillMaxWidth()) {
        // 4dp offset comic shadow
        Box(modifier = Modifier.matchParentSize()
            .offset(y = 4.dp)
            .background(ScrapbookDark.copy(alpha = 0.10f)))

        // Glass panel
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(Color.White.copy(alpha = 0.92f))
                .border(BorderStroke(2.5.dp, ScrapbookDark))
        ) {
            // Animated green gradient stripe — per-page accent overlay
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                .background(Brush.horizontalGradient(listOf(
                    CGreenDeep, accentColor.copy(alpha = 0.9f), CGreenMint,
                    accentColor.copy(alpha = 0.9f), CGreenDeep
                ))))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Menu button — green circle, comic shadow
                Box {
                    Box(modifier = Modifier.size(40.dp).offset(x = 3.dp, y = 3.dp).clip(CircleShape).background(ScrapbookDark.copy(alpha = 0.14f)))
                    Box(
                        modifier = Modifier.scale(menuScale).size(40.dp).clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint)))
                            .border(2.dp, ScrapbookDark, CircleShape)
                            .clickable { onNavigationIconClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Menu, "Menu", tint = ScrapbookDark, modifier = Modifier.size(20.dp))
                    }
                }

                // Page emoji badge
                Box(
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.14f))
                        .border(1.5.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(pageEmoji, fontSize = 14.sp)
                }

                // Title + subtitle (swipe ↑↑↓↓←→←→ here for a secret)
                Column(modifier = Modifier.weight(1f).konamiCode()) {
                    // Shimmer title
                    Box {
                        Text(currentScreenLabel.uppercase(),
                            color = ScrapbookDark, fontFamily = BangersFontFamily,
                            fontSize = 21.sp, letterSpacing = 2.sp)
                        Text(
                            currentScreenLabel.uppercase(),
                            fontFamily = BangersFontFamily, fontSize = 21.sp, letterSpacing = 2.sp,
                            style = androidx.compose.ui.text.TextStyle(brush = Brush.linearGradient(
                                colors = listOf(Color.Transparent, accentColor.copy(alpha = 0.6f), Color.Transparent),
                                start  = androidx.compose.ui.geometry.Offset(shimmerX - 120f, 0f),
                                end    = androidx.compose.ui.geometry.Offset(shimmerX + 120f, 0f)
                            ))
                        )
                    }
                    if (config != null) {
                        Text(
                            text = if (isPlayingMusic && currentScreenLabel.uppercase() == "ALBUMS")
                                "♪ ${selectedTrack?.title ?: nowPlaying?.title ?: config.subtitle}"
                            else config.subtitle,
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookDark.copy(alpha = 0.45f),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right side — now playing pill OR bell + avatar
                when {
                    isPlayingMusic && nowPlaying != null -> {
                        val npT = rememberInfiniteTransition(label = "barNpPulse")
                        val npAlpha by npT.animateFloat(0.5f, 1f,
                            infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "barNpPulseA")
                        Box {
                            Box(modifier = Modifier.matchParentSize().offset(x = 2.dp, y = 2.dp).clip(RoundedCornerShape(10.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                    .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint)))
                                    .border(1.5.dp, ScrapbookDark, RoundedCornerShape(10.dp))
                                    .clickable { onNowPlayingClick() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(1.dp), modifier = Modifier.height(12.dp)) {
                                        eqHeights.forEachIndexed { i, h ->
                                            val hVal by h
                                            Box(modifier = Modifier.width(2.dp).height(hVal.dp).clip(RoundedCornerShape(1.dp)).background(ScrapbookDark.copy(alpha = 0.6f + i * 0.1f)))
                                        }
                                    }
                                    Text(selectedTrack?.title?.take(10) ?: "PLAYING",
                                        fontFamily = BangersFontFamily, color = ScrapbookDark,
                                        fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                    else -> {
                        // Notification bell with comic shadow
                        Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.size(36.dp).offset(x = 2.dp, y = 2.dp).clip(CircleShape).background(ScrapbookDark.copy(alpha = 0.12f)))
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.92f))
                                    .border(2.dp, ScrapbookDark, CircleShape)
                                    .clickable { onNotificationsTap() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Notifications, null, tint = accentColor, modifier = Modifier.size(18.dp))
                            }
                            if (unreadNotifCount > 0) {
                                Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp)
                                    .size(16.dp).clip(CircleShape)
                                    .background(CAcRed).border(1.5.dp, ScrapbookDark, CircleShape),
                                    contentAlignment = Alignment.Center) {
                                    Text(if (unreadNotifCount > 9) "9+" else "$unreadNotifCount",
                                        fontFamily = BangersFontFamily, color = Color.White, fontSize = 8.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        // Avatar — comic shadow circle
                        Box {
                            Box(modifier = Modifier.size(36.dp).offset(x = 2.dp, y = 2.dp).clip(CircleShape).background(ScrapbookDark.copy(alpha = 0.14f)))
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint)))
                                    .border(2.dp, ScrapbookDark, CircleShape)
                                    .clickable { onNavigateToProfile() },
                                contentAlignment = Alignment.Center
                            ) {
                                if (firebaseProfile?.habboUsername?.isNotBlank() == true) {
                                    AsyncImage(
                                        model = "https://www.${firebaseProfile?.habboRegion ?: "habbo.com"}/habbo-imaging/avatarimage?user=${firebaseProfile?.habboUsername}&action=sit&direction=2&head_direction=3&gesture=sml&size=m",
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    )
                                } else if (currentUser != null) {
                                    Text(
                                        text = (firebaseProfile?.username ?: currentUser?.email ?: "?").take(1).uppercase(),
                                        fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp
                                    )
                                } else {
                                    Icon(Icons.Filled.Person, null, tint = ScrapbookDark, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ─── Per-page header copy (title / subtitle / marquee strip) ─────────────────

fun pageTitleFor(label: String): String = when (label) {
    "GAMES" -> "GAME DATABASE"
    "SUPPORT" -> "SUPPORT"
    "RETROBYTES" -> "RETROBYTES"
    else -> label
}

fun pageSubtitleFor(label: String): String = when (label) {
    "HOME" -> "Your daily dose of retro"
    "ALBUMS" -> "Soundtracks, chiptunes & OSTs"
    "MAGAZINES" -> "Classic issues, scanned & shelved"
    "STREAMS" -> "Live retro gaming right now"
    "GAMES" -> "Cartridges, classics & your collection"
    "EVENTS" -> "Releases, countdowns & meetups"
    "MARKETPLACE" -> "Find it, price it, trade it"
    "RETROBYTES" -> "Quick retro shorts"
    "FAVORITES" -> "Everything you bookmarked"
    "MESSAGES" -> "Retro gaming chat"
    "SUPPORT" -> "Tickets, petitions & help"
    else -> "RetroHub"
}

fun pageMarqueeFor(label: String): String = when (label) {
    "HOME" -> "★ RETROHUB  •  STORIES  •  COMMUNITY FEED  •  GAME OF THE DAY  •  PRESS START"
    "DISCOVER" -> "🔍 DISCOVER  •  NEWS  •  DEALS  •  PLAYERS  •  DAILY QUESTS  •  WARP ZONE"
    "MESSAGES" -> "💬 MESSAGES  •  CHAT  •  SQUAD UP  •  SEND GIFS  •  STAY CONNECTED"
    "ARTICLES" -> "✍️ ARTICLES  •  COMMUNITY STORIES  •  REVIEWS  •  OPINIONS  •  LIVE NEWS"
    "ALBUMS" -> "🎵 ALBUMS  •  SOUNDTRACKS  •  CHIPTUNES  •  OSTS  •  PRESS PLAY"
    "MAGAZINES" -> "📰 MAGAZINES  •  CLASSIC ISSUES  •  SHELVES  •  SCANS  •  NOSTALGIA"
    "STREAMS" -> "📺 STREAMS  •  LIVE NOW  •  SPEEDRUNS  •  VIDEOS  •  CLIPS"
    "GAMES" -> "👾 GAME DATABASE  •  CARTRIDGES  •  CLASSICS  •  MY COLLECTION  •  HIGHER OR LOWER"
    "EVENTS" -> "📅 EVENTS  •  RELEASES  •  COUNTDOWNS  •  TOURNAMENTS  •  MEETUPS"
    "MARKETPLACE" -> "🛒 MARKETPLACE  •  FIND IT  •  DEALS  •  COLLECTOR FINDS  •  TRADE"
    "RETROBYTES" -> "📱 RETROBYTES  •  SHORTS  •  CLIPS  •  QUICK HITS"
    "FAVORITES" -> "🔖 FAVORITES  •  ALBUMS  •  MAGAZINES  •  ARTICLES"
    "SUPPORT" -> "🛟 HELP DESK  •  REPORT BUGS  •  REPORT PLAYERS  •  BAN APPEALS  •  PETITIONS  •  WE'RE LISTENING"
    else -> "★ RETROHUB  •  PRESS START"
}

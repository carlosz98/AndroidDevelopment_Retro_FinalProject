package com.example.hubretro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

data class DiscoverResult(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: DiscoverCategory
)

enum class DiscoverCategory(val label: String, val color: androidx.compose.ui.graphics.Color) {
    USER("USER", ScrapbookDark),
    MAGAZINE("MAGAZINE", ScrapbookDark),
    ALBUM("ALBUM", ScrapbookDark),
    ARTICLE("ARTICLE", ScrapbookDark)
}

// ✅ Filter options
val discoverFilters = listOf("ALL", "PEOPLE", "ARTICLES", "MAGAZINES", "ALBUMS", "LIVE")

@Composable
fun DiscoverScreen(
    authViewModel: AuthViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(),
    streamsViewModel: StreamsViewModel = viewModel(),
    onNavigateToAlbums: () -> Unit = {},
    onNavigateToMagazines: () -> Unit = {},
    onNavigateToArticles: () -> Unit = {},
    onNavigateToStreams: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val followingUids by authViewModel.followingUids.collectAsState()
    val twitchState by streamsViewModel.twitchStreams.collectAsState()
    val communityStreamers by streamsViewModel.communityStreamers.collectAsState()
    val allUsers by authViewModel.allUsers.collectAsState()

    // ✅ 1. Filter state
    var discoverFilter by remember { mutableStateOf("ALL") }

    var searchQuery by remember { mutableStateOf("") }
    var hasSearched by remember { mutableStateOf(false) }
    var isSearchingUsers by remember { mutableStateOf(false) }
    var realUsers by remember { mutableStateOf<List<UserProfileData>>(emptyList()) }
    var selectedUser by remember { mutableStateOf<UserProfileData?>(null) }

    var trendingUsers by remember { mutableStateOf<List<UserProfileData>>(emptyList()) }
    var recentArticles by remember { mutableStateOf<List<ArticleItem>>(emptyList()) }
    var featuredArticle by remember { mutableStateOf<ArticleItem?>(null) }
    var topPlayers by remember { mutableStateOf<List<UserProfileData>>(emptyList()) }
    var isLoadingTrending by remember { mutableStateOf(true) }

    // ✅ Live streamers from Twitch state
    val liveStreamers = remember(twitchState, communityStreamers) {
        if (twitchState is StreamsState.Success<*>) {
            @Suppress("UNCHECKED_CAST")
            val streams = (twitchState as StreamsState.Success<TwitchStream>).data
            communityStreamers.filter { streamer ->
                streams.any { it.userName.lowercase() == streamer.twitchUsername.lowercase() }
            }.take(3)
        } else emptyList()
    }

    LaunchedEffect(Unit) {
        isLoadingTrending = true
        try {
            val firestore = FirebaseFirestore.getInstance()

            // Trending users
            val usersDoc = firestore.collection("users")
                .orderBy("followersCount", Query.Direction.DESCENDING)
                .limit(8).get().await()
            val allFetched = usersDoc.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                if (doc.id == currentUser?.uid) return@mapNotNull null
                UserProfileData(
                    uid = doc.id,
                    username = data["username"] as? String ?: "",
                    userHandle = data["userHandle"] as? String ?: "",
                    bio = data["bio"] as? String ?: "",
                    email = data["email"] as? String ?: "",
                    profilePictureUrl = data["profilePictureUrl"] as? String ?: "",
                    bannerUrl = data["bannerUrl"] as? String ?: "",
                    followersCount = (data["followersCount"] as? Long)?.toInt() ?: 0,
                    followingCount = (data["followingCount"] as? Long)?.toInt() ?: 0,
                    setupComplete = data["setupComplete"] as? Boolean ?: false,
                    topGames = (data["topGames"] as? List<*>)
                        ?.filterIsInstance<Map<String, Any>>() ?: emptyList(),
                    topSoundtracks = (data["topSoundtracks"] as? List<*>)
                        ?.filterIsInstance<Map<String, Any>>() ?: emptyList(),
                    twitchUsername = data["twitchUsername"] as? String ?: "",
                    youtubeUsername = data["youtubeUsername"] as? String ?: ""
                )
            }
            trendingUsers = allFetched.take(5)
            topPlayers = allFetched.take(3) // ✅ 6. Top players

            // ✅ 2. Featured article — most viewed
            try {
                val featuredDoc = firestore.collection("articles")
                    .orderBy("viewCount", Query.Direction.DESCENDING)
                    .limit(1).get().await()
                featuredArticle = featuredDoc.documents.firstOrNull()?.let { doc ->
                    val data = doc.data ?: return@let null
                    ArticleItem(
                        id = doc.id,
                        title = data["title"] as? String ?: "",
                        snippet = data["snippet"] as? String ?: "",
                        fullContent = data["fullContent"] as? String ?: "",
                        author = data["authorUsername"] as? String,
                        imageUrl = data["headerImageUrl"] as? String
                    )
                }?.takeIf { it.title.isNotBlank() }
            } catch (e: Exception) {
                // viewCount index may not exist — fall back to recent
                val fallbackDoc = firestore.collection("articles")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1).get().await()
                featuredArticle = fallbackDoc.documents.firstOrNull()?.let { doc ->
                    val data = doc.data ?: return@let null
                    ArticleItem(
                        id = doc.id,
                        title = data["title"] as? String ?: "",
                        snippet = data["snippet"] as? String ?: "",
                        fullContent = data["fullContent"] as? String ?: "",
                        author = data["authorUsername"] as? String,
                        imageUrl = data["headerImageUrl"] as? String
                    )
                }?.takeIf { it.title.isNotBlank() }
            }

            // Recent articles
            val articlesDoc = firestore.collection("articles")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(6).get().await()
            recentArticles = articlesDoc.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                ArticleItem(
                    id = doc.id,
                    title = data["title"] as? String ?: "",
                    snippet = data["snippet"] as? String ?: "",
                    fullContent = data["fullContent"] as? String ?: "",
                    author = data["authorUsername"] as? String,
                    imageUrl = data["headerImageUrl"] as? String
                )
            }.filter { it.title.isNotBlank() }

        } catch (e: Exception) { } finally {
            isLoadingTrending = false
        }

        // Load community streamers
        authViewModel.fetchAllUsers()
    }

    LaunchedEffect(allUsers) {
        streamsViewModel.loadCommunityStreamers(allUsers)
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            delay(600)
            isSearchingUsers = true
            try {
                val firestore = FirebaseFirestore.getInstance()
                val byUsername = firestore.collection("users")
                    .whereGreaterThanOrEqualTo("username", searchQuery.lowercase())
                    .whereLessThanOrEqualTo("username", searchQuery.lowercase() + "\uf8ff")
                    .limit(10).get().await()
                val byHandle = firestore.collection("users")
                    .whereGreaterThanOrEqualTo("userHandle", "@${searchQuery.lowercase()}")
                    .whereLessThanOrEqualTo("userHandle", "@${searchQuery.lowercase()}\uf8ff")
                    .limit(10).get().await()
                val combined = (byUsername.documents + byHandle.documents)
                    .distinctBy { it.id }
                    .mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        UserProfileData(
                            uid = doc.id,
                            username = data["username"] as? String ?: "",
                            userHandle = data["userHandle"] as? String ?: "",
                            bio = data["bio"] as? String ?: "",
                            email = data["email"] as? String ?: "",
                            profilePictureUrl = data["profilePictureUrl"] as? String ?: "",
                            bannerUrl = data["bannerUrl"] as? String ?: "",
                            followersCount = (data["followersCount"] as? Long)?.toInt() ?: 0,
                            followingCount = (data["followingCount"] as? Long)?.toInt() ?: 0,
                            setupComplete = data["setupComplete"] as? Boolean ?: false,
                            topGames = (data["topGames"] as? List<*>)
                                ?.filterIsInstance<Map<String, Any>>() ?: emptyList(),
                            topSoundtracks = (data["topSoundtracks"] as? List<*>)
                                ?.filterIsInstance<Map<String, Any>>() ?: emptyList()
                        )
                    }.filter { it.uid != currentUser?.uid }
                realUsers = combined
            } catch (e: Exception) {
                realUsers = emptyList()
            } finally {
                isSearchingUsers = false
            }
        } else {
            realUsers = emptyList()
        }
    }

    val localResults: List<DiscoverResult> = remember {
        val magazineResults = sampleMagazineCovers.map {
            DiscoverResult(id = "mag_${it.id}", title = it.title, subtitle = "Virtual Magazine", category = DiscoverCategory.MAGAZINE)
        }
        val albumResults = sampleAlbums.map {
            DiscoverResult(id = "alb_${it.id}", title = it.title, subtitle = it.artist, category = DiscoverCategory.ALBUM)
        }
        val articleResults = sampleArticles.map {
            DiscoverResult(id = "art_${it.id}", title = it.title, subtitle = it.author ?: "Unknown author", category = DiscoverCategory.ARTICLE)
        }
        magazineResults + albumResults + articleResults
    }

    val filteredLocal = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else localResults.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.subtitle.contains(searchQuery, ignoreCase = true)
        }
    }

    val groupedLocal = filteredLocal.groupBy { it.category }

    if (selectedUser != null) {
        UserProfileViewScreen(
            user = selectedUser!!,
            authViewModel = authViewModel,
            onBack = { selectedUser = null }
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScrapbookCream)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ✅ Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScrapbookYellow)
                    .border(BorderStroke(2.dp, ScrapbookBorder))
                    .padding(top = 16.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "DISCOVER",
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 32.sp,
                        letterSpacing = 2.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Explore users, articles, magazines & more",
                        fontFamily = NunitoFontFamily,
                        color = ScrapbookDark.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ✅ Search bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScrapbookCream)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        hasSearched = it.isNotBlank()
                    },
                    placeholder = {
                        Text(
                            "Search anything...",
                            fontFamily = NunitoFontFamily,
                            fontSize = 14.sp,
                            color = ScrapbookTextMuted
                        )
                    },
                    leadingIcon = {
                        if (isSearchingUsers) {
                            CircularProgressIndicator(
                                color = ScrapbookDark,
                                modifier = Modifier.size(20.dp).padding(2.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = ScrapbookDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                hasSearched = false
                                realUsers = emptyList()
                                focusManager.clearFocus()
                            }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Clear",
                                    tint = ScrapbookTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    textStyle = TextStyle(
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        color = ScrapbookTextDark
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ScrapbookDark,
                        unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f),
                        focusedContainerColor = ScrapbookCardWhite,
                        unfocusedContainerColor = ScrapbookCardWhite,
                        cursorColor = ScrapbookDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ✅ 1. Filter strip — only shown when not searching
            if (!hasSearched) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ScrapbookCream)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(discoverFilters) { filter ->
                        val isSelected = discoverFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) ScrapbookDark else ScrapbookCardWhite
                                )
                                .border(
                                    2.dp,
                                    ScrapbookBorder,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { discoverFilter = filter }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = filter,
                                fontFamily = BangersFontFamily,
                                color = if (isSelected) ScrapbookYellow else ScrapbookDark,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            when {
                !hasSearched -> {
                    if (isLoadingTrending) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = ScrapbookYellowDark,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp,
                                top = 8.dp, bottom = 24.dp
                            )
                        ) {
                            // ✅ 2. Featured Article
                            if (featuredArticle != null &&
                                (discoverFilter == "ALL" || discoverFilter == "ARTICLES")
                            ) {
                                item {
                                    FeaturedArticleCard(article = featuredArticle!!)
                                }
                            }

                            // ✅ 5. Live Now section
                            if (liveStreamers.isNotEmpty() &&
                                (discoverFilter == "ALL" || discoverFilter == "LIVE")
                            ) {
                                item {
                                    DiscoverSectionRow(
                                        title = "LIVE NOW",
                                        emoji = "🔴",
                                        onSeeAll = { onNavigateToStreams() }
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(horizontal = 2.dp)
                                    ) {
                                        items(liveStreamers, key = { it.uid }) { streamer ->
                                            LiveStreamerCard(streamer = streamer)
                                        }
                                    }
                                }
                            }

                            // ✅ 6. Trending Players stacked card
                            if (topPlayers.isNotEmpty() &&
                                (discoverFilter == "ALL" || discoverFilter == "PEOPLE")
                            ) {
                                item {
                                    DiscoverSectionRow(
                                        title = "TOP PLAYERS",
                                        emoji = "🏆",
                                        onSeeAll = null
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    TrendingPlayersCard(
                                        users = topPlayers,
                                        followingUids = followingUids,
                                        currentUid = currentUser?.uid ?: "",
                                        onFollowClick = { user ->
                                            if (followingUids.contains(user.uid))
                                                authViewModel.unfollowUser(user.uid)
                                            else
                                                authViewModel.followUser(user.uid)
                                        },
                                        onTap = { selectedUser = it }
                                    )
                                }
                            }

                            // Trending Users carousel
                            if (trendingUsers.isNotEmpty() &&
                                (discoverFilter == "ALL" || discoverFilter == "PEOPLE")
                            ) {
                                item {
                                    DiscoverSectionRow(
                                        title = "TRENDING USERS",
                                        emoji = "🔥",
                                        onSeeAll = null
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(horizontal = 2.dp)
                                    ) {
                                        items(trendingUsers, key = { it.uid }) { user ->
                                            ScrapbookTrendingUserCard(
                                                user = user,
                                                isFollowing = followingUids.contains(user.uid),
                                                onTap = { selectedUser = user },
                                                onFollowClick = {
                                                    if (followingUids.contains(user.uid))
                                                        authViewModel.unfollowUser(user.uid)
                                                    else
                                                        authViewModel.followUser(user.uid)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // ✅ 4. Articles horizontal carousel
                            if (recentArticles.isNotEmpty() &&
                                (discoverFilter == "ALL" || discoverFilter == "ARTICLES")
                            ) {
                                item {
                                    DiscoverSectionRow(
                                        title = "RECENT ARTICLES",
                                        emoji = "📝",
                                        onSeeAll = { onNavigateToArticles() }
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    // ✅ Horizontal carousel instead of stacked list
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        contentPadding = PaddingValues(horizontal = 2.dp)
                                    ) {
                                        items(recentArticles, key = { "art_${it.id}" }) { article ->
                                            ArticleMiniCard(article = article)
                                        }
                                    }
                                }
                            }

                            // ✅ 4. Magazines horizontal carousel with SEE ALL
                            if (discoverFilter == "ALL" || discoverFilter == "MAGAZINES") {
                                item {
                                    DiscoverSectionRow(
                                        title = "FEATURED MAGAZINES",
                                        emoji = "📰",
                                        onSeeAll = { onNavigateToMagazines() }
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(horizontal = 2.dp)
                                    ) {
                                        items(sampleMagazineCovers.take(6), key = { "mag_${it.id}" }) { mag ->
                                            ScrapbookTrendingMagazineCard(magazine = mag)
                                        }
                                    }
                                }
                            }

                            // ✅ 4. Albums horizontal carousel with SEE ALL
                            if (discoverFilter == "ALL" || discoverFilter == "ALBUMS") {
                                item {
                                    DiscoverSectionRow(
                                        title = "FEATURED ALBUMS",
                                        emoji = "🎵",
                                        onSeeAll = { onNavigateToAlbums() }
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(horizontal = 2.dp)
                                    ) {
                                        items(sampleAlbums.take(6), key = { "alb_${it.id}" }) { album ->
                                            ScrapbookTrendingAlbumCard(album = album)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                realUsers.isEmpty() && filteredLocal.isEmpty() && !isSearchingUsers -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No results for \"$searchQuery\"",
                                fontFamily = NunitoFontFamily,
                                color = ScrapbookTextMuted,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp, bottom = 16.dp
                        )
                    ) {
                        if (realUsers.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "USERS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Divider(modifier = Modifier.weight(1f), color = ScrapbookBorder.copy(alpha = 0.2f))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(ScrapbookYellow)
                                            .border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = "${realUsers.size}", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                                    }
                                }
                            }
                            items(realUsers, key = { it.uid }) { user ->
                                ScrapbookDiscoverUserCard(
                                    user = user,
                                    isFollowing = followingUids.contains(user.uid),
                                    isCurrentUser = user.uid == currentUser?.uid,
                                    onFollowClick = {
                                        if (followingUids.contains(user.uid))
                                            authViewModel.unfollowUser(user.uid)
                                        else
                                            authViewModel.followUser(user.uid)
                                    },
                                    onTap = { selectedUser = user }
                                )
                            }
                        }

                        DiscoverCategory.values()
                            .filter { it != DiscoverCategory.USER }
                            .forEach { category ->
                                val categoryResults = groupedLocal[category]
                                if (!categoryResults.isNullOrEmpty()) {
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = category.label, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Divider(modifier = Modifier.weight(1f), color = ScrapbookBorder.copy(alpha = 0.2f))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(ScrapbookYellow)
                                                    .border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(text = "${categoryResults.size}", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                    items(categoryResults, key = { it.id }) { result ->
                                        ScrapbookDiscoverResultCard(result = result)
                                    }
                                }
                            }
                    }
                }
            }
        }
    }
}

// ✅ 3. Section header with SEE ALL button
@Composable
fun DiscoverSectionRow(
    title: String,
    emoji: String,
    onSeeAll: (() -> Unit)?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = emoji, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontFamily = BangersFontFamily,
            color = ScrapbookDark,
            fontSize = 22.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.weight(1f)
        )
        if (onSeeAll != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(ScrapbookYellow)
                    .border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp))
                    .clickable { onSeeAll() }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "SEE ALL →",
                    fontFamily = BangersFontFamily,
                    color = ScrapbookDark,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ✅ 2. Featured Article Card
@Composable
fun FeaturedArticleCard(article: ArticleItem) {
    Box {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 14.dp,
            shadowOffset = 5.dp
        ) {
            Column {
                // Image with overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                ) {
                    if (!article.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = article.imageUrl,
                            contentDescription = article.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(ScrapbookPaper),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📰", fontSize = 48.sp)
                        }
                    }
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )
                    // Featured badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ScrapbookYellow)
                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "⭐ FEATURED",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 12.sp
                        )
                    }
                    // Title on image
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = article.title,
                            fontFamily = BangersFontFamily,
                            color = Color.White,
                            fontSize = 20.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 24.sp
                        )
                        article.author?.let {
                            Text(
                                text = "by $it",
                                fontFamily = NunitoFontFamily,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                // Snippet
                if (article.snippet.isNotBlank()) {
                    Text(
                        text = article.snippet,
                        fontFamily = NunitoFontFamily,
                        color = ScrapbookTextMuted,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

// ✅ 4. Article Mini Card for horizontal carousel
@Composable
fun ArticleMiniCard(article: ArticleItem) {
    Box(modifier = Modifier.width(180.dp)) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 10.dp,
            shadowOffset = 3.dp
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                        .background(ScrapbookPaper),
                    contentAlignment = Alignment.Center
                ) {
                    if (!article.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = article.imageUrl,
                            contentDescription = article.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("📝", fontSize = 32.sp)
                    }
                }
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = article.title,
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                    article.author?.let {
                        Text(
                            text = "by $it",
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ✅ 5. Live Streamer Card
@Composable
fun LiveStreamerCard(streamer: CommunityStreamer) {
    Box(modifier = Modifier.width(120.dp)) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 12.dp,
            shadowOffset = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(ScrapbookPaper)
                            .border(2.dp, Color.Red, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (streamer.profilePicUrl.isNotBlank()) {
                            AsyncImage(
                                model = streamer.profilePicUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = streamer.username.take(1).uppercase(),
                                fontFamily = BangersFontFamily,
                                color = ScrapbookDark,
                                fontSize = 22.sp
                            )
                        }
                    }
                    // Live dot
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                            .border(2.dp, Color.White, CircleShape)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = streamer.username,
                    fontFamily = BangersFontFamily,
                    color = ScrapbookDark,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Red)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "🔴 LIVE",
                        fontFamily = BangersFontFamily,
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
                if (streamer.twitchUsername.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "/${streamer.twitchUsername}",
                        fontFamily = NunitoFontFamily,
                        color = Color(0xFF9146FF),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ✅ 6. Trending Players stacked card
@Composable
fun TrendingPlayersCard(
    users: List<UserProfileData>,
    followingUids: Set<String>,
    currentUid: String,
    onFollowClick: (UserProfileData) -> Unit,
    onTap: (UserProfileData) -> Unit
) {
    Box {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 14.dp,
            shadowOffset = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                users.forEachIndexed { index, user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTap(user) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rank badge
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    when (index) {
                                        0 -> Color(0xFFFFD700) // gold
                                        1 -> Color(0xFFC0C0C0) // silver
                                        else -> Color(0xFFCD7F32) // bronze
                                    }
                                )
                                .border(2.dp, ScrapbookBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontFamily = BangersFontFamily,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(ScrapbookPaper)
                                .border(2.dp, ScrapbookBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!user.profilePictureUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = user.profilePictureUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = user.username.take(1).uppercase(),
                                    fontFamily = BangersFontFamily,
                                    color = ScrapbookDark,
                                    fontSize = 18.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.username.uppercase(),
                                fontFamily = BangersFontFamily,
                                color = ScrapbookDark,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatCount(user.followersCount)} followers",
                                fontFamily = NunitoFontFamily,
                                color = ScrapbookTextMuted,
                                fontSize = 12.sp
                            )
                        }

                        // Follow button
                        if (user.uid != currentUid) {
                            val isFollowing = followingUids.contains(user.uid)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isFollowing) ScrapbookPaper else ScrapbookDark
                                    )
                                    .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                                    .clickable { onFollowClick(user) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (isFollowing) "FOLLOWING" else "FOLLOW",
                                    fontFamily = BangersFontFamily,
                                    fontSize = 12.sp,
                                    color = if (isFollowing) ScrapbookDark else ScrapbookYellow
                                )
                            }
                        }
                    }

                    // Divider between rows
                    if (index < users.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = ScrapbookBorder.copy(alpha = 0.15f),
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}

// ✅ Scrapbook Discover Section Header (kept for backward compat)
@Composable
fun ScrapbookDiscoverHeader(title: String, emoji: String) {
    DiscoverSectionRow(title = title, emoji = emoji, onSeeAll = null)
}

// ✅ Trending User Card
@Composable
fun ScrapbookTrendingUserCard(
    user: UserProfileData,
    isFollowing: Boolean,
    onTap: () -> Unit,
    onFollowClick: () -> Unit
) {
    Box(modifier = Modifier.width(100.dp)) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth().clickable { onTap() },
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 12.dp,
            shadowOffset = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(ScrapbookPaper)
                        .border(2.dp, ScrapbookBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!user.profilePictureUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = user.profilePictureUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = ScrapbookDark.copy(alpha = 0.4f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = user.username,
                    fontFamily = BangersFontFamily,
                    color = ScrapbookDark,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${formatCount(user.followersCount)} followers",
                    fontFamily = NunitoFontFamily,
                    color = ScrapbookTextMuted,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isFollowing) ScrapbookPaper else ScrapbookDark)
                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(6.dp))
                        .clickable { onFollowClick() }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isFollowing) "FOLLOWING" else "FOLLOW",
                        fontFamily = BangersFontFamily,
                        fontSize = 11.sp,
                        color = if (isFollowing) ScrapbookDark else ScrapbookYellow
                    )
                }
            }
        }
    }
}

// ✅ Trending Article Card
@Composable
fun ScrapbookTrendingArticleCard(article: ArticleItem) {
    Box(modifier = Modifier.padding(vertical = 4.dp)) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 10.dp,
            shadowOffset = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ScrapbookPaper)
                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!article.imageUrl.isNullOrBlank()) {
                        AsyncImage(model = article.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else if (article.imageResId != null) {
                        androidx.compose.foundation.Image(painter = painterResource(id = article.imageResId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Text("📝", fontSize = 24.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = article.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
                    article.author?.let {
                        Text(text = "by $it", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
                    }
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookYellow)
                        .border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(text = "ARTICLE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 10.sp)
                }
            }
        }
    }
}

// ✅ Trending Magazine Card
@Composable
fun ScrapbookTrendingMagazineCard(magazine: MagazineCover) {
    Box(modifier = Modifier.width(90.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 8.dp, shadowOffset = 3.dp) {
            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                        .clip(RoundedCornerShape(6.dp)).background(ScrapbookPaper)
                ) {
                    when {
                        magazine.coverImageResId != null -> androidx.compose.foundation.Image(
                            painter = painterResource(id = magazine.coverImageResId),
                            contentDescription = magazine.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        magazine.coverImageUrl != null -> AsyncImage(model = magazine.coverImageUrl, contentDescription = magazine.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("📰", fontSize = 28.sp) }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = magazine.title, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextDark, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, lineHeight = 12.sp, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ✅ Trending Album Card
@Composable
fun ScrapbookTrendingAlbumCard(album: Album) {
    Box(modifier = Modifier.width(90.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 8.dp, shadowOffset = 3.dp) {
            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(6.dp)).background(ScrapbookPaper), contentAlignment = Alignment.Center) {
                    if (album.coverImageResId != null) {
                        androidx.compose.foundation.Image(painter = painterResource(id = album.coverImageResId), contentDescription = album.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Text("🎵", fontSize = 28.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = album.title, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextDark, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, lineHeight = 12.sp, modifier = Modifier.fillMaxWidth())
                Text(text = album.artist, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ✅ User card in search results
@Composable
fun ScrapbookDiscoverUserCard(
    user: UserProfileData,
    isFollowing: Boolean,
    isCurrentUser: Boolean,
    onFollowClick: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.padding(vertical = 4.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth().clickable { onTap() }, backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp, shadowOffset = 3.dp) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(ScrapbookPaper).border(2.dp, ScrapbookBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!user.profilePictureUrl.isNullOrBlank()) {
                        AsyncImage(model = user.profilePictureUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = ScrapbookDark.copy(alpha = 0.4f), modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = user.username.uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = user.userHandle, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (user.bio.isNotBlank()) {
                        Text(text = user.bio, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (!isCurrentUser) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(if (isFollowing) ScrapbookPaper else ScrapbookDark)
                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                            .clickable { onFollowClick() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = if (isFollowing) "FOLLOWING" else "FOLLOW", fontFamily = BangersFontFamily, fontSize = 13.sp, color = if (isFollowing) ScrapbookDark else ScrapbookYellow)
                    }
                }
            }
        }
    }
}

// ✅ Result card
@Composable
fun ScrapbookDiscoverResultCard(result: DiscoverResult, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(vertical = 4.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth().clickable { }, backgroundColor = ScrapbookCardWhite, cornerRadius = 10.dp, shadowOffset = 3.dp) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(ScrapbookDark, CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = result.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = result.subtitle, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookYellow).border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text(text = result.category.label, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp)
                }
            }
        }
    }
}

// Backward compat wrappers
@Composable
fun TrendingSectionHeader(title: String, color: Color) { DiscoverSectionRow(title = title, emoji = "🔥", onSeeAll = null) }

@Composable
fun TrendingUserCard(user: UserProfileData, isFollowing: Boolean, onTap: () -> Unit, onFollowClick: () -> Unit) {
    ScrapbookTrendingUserCard(user = user, isFollowing = isFollowing, onTap = onTap, onFollowClick = onFollowClick)
}

@Composable
fun TrendingArticleCard(article: ArticleItem) { ScrapbookTrendingArticleCard(article = article) }

@Composable
fun TrendingMagazineCard(magazine: MagazineCover) { ScrapbookTrendingMagazineCard(magazine = magazine) }

@Composable
fun TrendingAlbumCard(album: Album) { ScrapbookTrendingAlbumCard(album = album) }

@Composable
fun DiscoverUserCard(user: UserProfileData, isFollowing: Boolean, isCurrentUser: Boolean, onFollowClick: () -> Unit, onTap: () -> Unit, modifier: Modifier = Modifier) {
    ScrapbookDiscoverUserCard(user = user, isFollowing = isFollowing, isCurrentUser = isCurrentUser, onFollowClick = onFollowClick, onTap = onTap, modifier = modifier)
}

@Composable
fun DiscoverResultCard(result: DiscoverResult, modifier: Modifier = Modifier) { ScrapbookDiscoverResultCard(result = result, modifier = modifier) }
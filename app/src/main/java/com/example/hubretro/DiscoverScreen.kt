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
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
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
    USER("USER", VaporwaveCyan),
    MAGAZINE("MAGAZINE", VaporwavePink),
    ALBUM("ALBUM", SynthwaveOrange),
    ARTICLE("ARTICLE", VaporwaveGreen)
}

@Composable
fun DiscoverScreen(
    authViewModel: AuthViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val followingUids by authViewModel.followingUids.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var hasSearched by remember { mutableStateOf(false) }
    var isSearchingUsers by remember { mutableStateOf(false) }
    var realUsers by remember { mutableStateOf<List<UserProfileData>>(emptyList()) }
    var selectedUser by remember { mutableStateOf<UserProfileData?>(null) }

    // Trending data
    var trendingUsers by remember { mutableStateOf<List<UserProfileData>>(emptyList()) }
    var recentArticles by remember { mutableStateOf<List<ArticleItem>>(emptyList()) }
    var isLoadingTrending by remember { mutableStateOf(true) }

    // Fetch trending data on load
    LaunchedEffect(Unit) {
        isLoadingTrending = true
        try {
            val firestore = FirebaseFirestore.getInstance()

            // Trending users — most followers
            val usersDoc = firestore.collection("users")
                .orderBy("followersCount", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .await()
            trendingUsers = usersDoc.documents.mapNotNull { doc ->
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
                        ?.filterIsInstance<Map<String, Any>>() ?: emptyList()
                )
            }

            // Recent articles from Firestore
            val articlesDoc = firestore.collection("articles")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .await()
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

        } catch (e: Exception) {
            // silently fail
        } finally {
            isLoadingTrending = false
        }
    }

    // Search real users from Firestore with debounce
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            delay(600)
            isSearchingUsers = true
            try {
                val firestore = FirebaseFirestore.getInstance()
                val byUsername = firestore.collection("users")
                    .whereGreaterThanOrEqualTo("username", searchQuery.lowercase())
                    .whereLessThanOrEqualTo("username", searchQuery.lowercase() + "\uf8ff")
                    .limit(10)
                    .get()
                    .await()
                val byHandle = firestore.collection("users")
                    .whereGreaterThanOrEqualTo("userHandle", "@${searchQuery.lowercase()}")
                    .whereLessThanOrEqualTo("userHandle", "@${searchQuery.lowercase()}\uf8ff")
                    .limit(10)
                    .get()
                    .await()

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
                    }
                    .filter { it.uid != currentUser?.uid }
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

    // Local content results
    val localResults: List<DiscoverResult> = remember {
        val magazineResults = sampleMagazineCovers.map {
            DiscoverResult(
                id = "mag_${it.id}",
                title = it.title,
                subtitle = "Virtual Magazine",
                category = DiscoverCategory.MAGAZINE
            )
        }
        val albumResults = sampleAlbums.map {
            DiscoverResult(
                id = "alb_${it.id}",
                title = it.title,
                subtitle = it.artist,
                category = DiscoverCategory.ALBUM
            )
        }
        val articleResults = sampleArticles.map {
            DiscoverResult(
                id = "art_${it.id}",
                title = it.title,
                subtitle = it.author ?: "Unknown author",
                category = DiscoverCategory.ARTICLE
            )
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

    // Show user profile view
    if (selectedUser != null) {
        UserProfileViewScreen(
            user = selectedUser!!,
            authViewModel = authViewModel,
            onBack = { selectedUser = null }
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "DISCOVER",
            style = TextStyle(
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                shadow = Shadow(
                    color = VaporwaveCyan.copy(alpha = 0.8f),
                    offset = Offset(x = 4f, y = 4f),
                    blurRadius = 8f
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        Text(
            text = "Search users, magazines, albums & articles",
            style = TextStyle(
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite.copy(alpha = 0.5f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        )

        // --- Search Bar ---
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                hasSearched = it.isNotBlank()
            },
            placeholder = {
                Text(
                    "Search anything...",
                    fontFamily = RetroFontFamily,
                    fontSize = 12.sp,
                    color = RetroTextOffWhite.copy(alpha = 0.4f)
                )
            },
            leadingIcon = {
                if (isSearchingUsers) {
                    CircularProgressIndicator(
                        color = VaporwaveCyan,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(2.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = VaporwaveCyan,
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
                            contentDescription = "Clear search",
                            tint = RetroTextOffWhite.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            textStyle = TextStyle(
                fontFamily = RetroFontFamily,
                fontSize = 13.sp,
                color = RetroTextOffWhite
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VaporwaveCyan,
                unfocusedBorderColor = RetroTextOffWhite.copy(alpha = 0.3f),
                focusedContainerColor = RetroDarkPurple.copy(alpha = 0.8f),
                unfocusedContainerColor = RetroDarkPurple.copy(alpha = 0.8f),
                cursorColor = VaporwaveCyan
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            // --- No search yet — show trending ---
            !hasSearched -> {
                if (isLoadingTrending) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = VaporwaveCyan,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // Trending Users
                        if (trendingUsers.isNotEmpty()) {
                            item {
                                TrendingSectionHeader(
                                    title = "TRENDING USERS",
                                    color = VaporwaveCyan
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    items(trendingUsers, key = { it.uid }) { user ->
                                        TrendingUserCard(
                                            user = user,
                                            isFollowing = followingUids.contains(user.uid),
                                            onTap = { selectedUser = user },
                                            onFollowClick = {
                                                if (followingUids.contains(user.uid)) {
                                                    authViewModel.unfollowUser(user.uid)
                                                } else {
                                                    authViewModel.followUser(user.uid)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Recent Community Articles
                        if (recentArticles.isNotEmpty()) {
                            item {
                                TrendingSectionHeader(
                                    title = "RECENT ARTICLES",
                                    color = VaporwaveGreen
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            items(recentArticles.take(3), key = { "trend_art_${it.id}" }) { article ->
                                TrendingArticleCard(article = article)
                            }
                        }

                        // Trending Magazines
                        item {
                            TrendingSectionHeader(
                                title = "FEATURED MAGAZINES",
                                color = VaporwavePink
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                items(sampleMagazineCovers.take(5), key = { "trend_mag_${it.id}" }) { mag ->
                                    TrendingMagazineCard(magazine = mag)
                                }
                            }
                        }

                        // Trending Albums
                        item {
                            TrendingSectionHeader(
                                title = "FEATURED ALBUMS",
                                color = SynthwaveOrange
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                items(sampleAlbums.take(5), key = { "trend_alb_${it.id}" }) { album ->
                                    TrendingAlbumCard(album = album)
                                }
                            }
                        }
                    }
                }
            }

            // --- No results ---
            realUsers.isEmpty() && filteredLocal.isEmpty() && !isSearchingUsers -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🔍", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No results for \"$searchQuery\"",
                            style = TextStyle(
                                fontFamily = RetroFontFamily,
                                color = RetroTextOffWhite.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }

            // --- Search results ---
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    if (realUsers.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "USERS",
                                    style = TextStyle(
                                        fontFamily = RetroFontFamily,
                                        color = VaporwaveCyan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Divider(
                                    modifier = Modifier.weight(1f),
                                    color = VaporwaveCyan.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${realUsers.size}",
                                    style = TextStyle(
                                        fontFamily = RetroFontFamily,
                                        color = VaporwaveCyan.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                        items(realUsers, key = { it.uid }) { user ->
                            DiscoverUserCard(
                                user = user,
                                isFollowing = followingUids.contains(user.uid),
                                isCurrentUser = user.uid == currentUser?.uid,
                                onFollowClick = {
                                    if (followingUids.contains(user.uid)) {
                                        authViewModel.unfollowUser(user.uid)
                                    } else {
                                        authViewModel.followUser(user.uid)
                                    }
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = category.label,
                                            style = TextStyle(
                                                fontFamily = RetroFontFamily,
                                                color = category.color,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Divider(
                                            modifier = Modifier.weight(1f),
                                            color = category.color.copy(alpha = 0.3f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${categoryResults.size}",
                                            style = TextStyle(
                                                fontFamily = RetroFontFamily,
                                                color = category.color.copy(alpha = 0.7f),
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                                items(categoryResults, key = { it.id }) { result ->
                                    DiscoverResultCard(result = result)
                                }
                            }
                        }
                }
            }
        }
    }
}

// --- Trending Section Header ---
@Composable
fun TrendingSectionHeader(title: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Filled.Whatshot,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontFamily = RetroFontFamily,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Divider(
            modifier = Modifier.weight(1f),
            color = color.copy(alpha = 0.3f)
        )
    }
}

// --- Trending User Card (compact horizontal card) ---
@Composable
fun TrendingUserCard(
    user: UserProfileData,
    isFollowing: Boolean,
    onTap: () -> Unit,
    onFollowClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(RetroDarkPurple.copy(alpha = 0.7f))
            .border(
                1.dp,
                if (isFollowing) VaporwaveCyan.copy(alpha = 0.5f)
                else RetroTextOffWhite.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onTap() }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(RetroDarkPurple)
                .border(2.dp, VaporwaveCyan.copy(alpha = 0.5f), CircleShape),
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
                    tint = RetroTextOffWhite.copy(alpha = 0.4f),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = user.username,
            fontFamily = RetroFontFamily,
            color = RetroTextOffWhite,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "${formatCount(user.followersCount)} followers",
            fontFamily = RetroFontFamily,
            color = VaporwaveCyan.copy(alpha = 0.7f),
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Button(
            onClick = onFollowClick,
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isFollowing) Color.Transparent else VaporwavePink
            ),
            border = if (isFollowing)
                BorderStroke(1.dp, VaporwaveCyan.copy(alpha = 0.6f)) else null,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
        ) {
            Text(
                text = if (isFollowing) "FOLLOWING" else "FOLLOW",
                fontFamily = RetroFontFamily,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = if (isFollowing) VaporwaveCyan else Color.White
            )
        }
    }
}

// --- Trending Article Card ---
@Composable
fun TrendingArticleCard(article: ArticleItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(RetroDarkPurple.copy(alpha = 0.6f))
            .border(
                1.dp,
                VaporwaveGreen.copy(alpha = 0.3f),
                RoundedCornerShape(10.dp)
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Article image or placeholder
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2A2A3A)),
            contentAlignment = Alignment.Center
        ) {
            if (!article.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (article.imageResId != null) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = article.imageResId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("📝", fontSize = 24.sp)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = article.title,
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            article.author?.let { author ->
                Text(
                    text = "by $author",
                    fontFamily = RetroFontFamily,
                    color = VaporwaveGreen.copy(alpha = 0.8f),
                    fontSize = 10.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .background(
                    VaporwaveGreen.copy(alpha = 0.15f),
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "ARTICLE",
                fontFamily = RetroFontFamily,
                color = VaporwaveGreen,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// --- Trending Magazine Card ---
@Composable
fun TrendingMagazineCard(magazine: MagazineCover) {
    Column(
        modifier = Modifier
            .width(90.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(RetroDarkPurple.copy(alpha = 0.6f))
            .border(1.dp, VaporwavePink.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF2A2A3A))
        ) {
            when {
                magazine.coverImageResId != null -> androidx.compose.foundation.Image(
                    painter = painterResource(id = magazine.coverImageResId),
                    contentDescription = magazine.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                magazine.coverImageUrl != null -> AsyncImage(
                    model = magazine.coverImageUrl,
                    contentDescription = magazine.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                else -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📰", fontSize = 28.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = magazine.title,
            fontFamily = RetroFontFamily,
            color = RetroTextOffWhite,
            fontSize = 9.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// --- Trending Album Card ---
@Composable
fun TrendingAlbumCard(album: Album) {
    Column(
        modifier = Modifier
            .width(90.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(RetroDarkPurple.copy(alpha = 0.6f))
            .border(1.dp, SynthwaveOrange.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF2A2A3A)),
            contentAlignment = Alignment.Center
        ) {
            if (album.coverImageResId != null) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = album.coverImageResId),
                    contentDescription = album.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("🎵", fontSize = 28.sp)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = album.title,
            fontFamily = RetroFontFamily,
            color = RetroTextOffWhite,
            fontSize = 9.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = album.artist,
            fontFamily = RetroFontFamily,
            color = SynthwaveOrange.copy(alpha = 0.7f),
            fontSize = 8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// --- Real User Card in Discover ---
@Composable
fun DiscoverUserCard(
    user: UserProfileData,
    isFollowing: Boolean,
    isCurrentUser: Boolean,
    onFollowClick: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RetroDarkPurple.copy(alpha = 0.7f))
            .border(
                BorderStroke(
                    1.dp,
                    if (isFollowing) VaporwaveCyan.copy(alpha = 0.5f)
                    else RetroTextOffWhite.copy(alpha = 0.15f)
                ),
                RoundedCornerShape(12.dp)
            )
            .clickable { onTap() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(RetroDarkPurple)
                .border(2.dp, VaporwavePink.copy(alpha = 0.5f), CircleShape),
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
                    tint = RetroTextOffWhite.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.username.uppercase(),
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = user.userHandle,
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = RetroTextSecondary,
                    fontSize = 11.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (user.bio.isNotBlank()) {
                Text(
                    text = user.bio,
                    style = TextStyle(
                        fontFamily = RetroFontFamily,
                        color = RetroTextOffWhite.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (!isCurrentUser) {
            Button(
                onClick = onFollowClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) Color.Transparent else VaporwavePink
                ),
                border = if (isFollowing)
                    BorderStroke(1.dp, VaporwaveCyan.copy(alpha = 0.7f)) else null,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = if (isFollowing) "FOLLOWING" else "FOLLOW",
                    fontFamily = RetroFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFollowing) VaporwaveCyan else Color.White
                )
            }
        }
    }
}

// --- Individual content result card ---
@Composable
fun DiscoverResultCard(
    result: DiscoverResult,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(RetroDarkPurple.copy(alpha = 0.7f))
            .border(
                BorderStroke(1.dp, result.category.color.copy(alpha = 0.4f)),
                RoundedCornerShape(10.dp)
            )
            .clickable { }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(result.category.color, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.title,
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = result.subtitle,
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite.copy(alpha = 0.6f),
                    fontSize = 11.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .background(
                    result.category.color.copy(alpha = 0.2f),
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = result.category.label,
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = result.category.color,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
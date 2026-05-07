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

    var trendingUsers by remember { mutableStateOf<List<UserProfileData>>(emptyList()) }
    var recentArticles by remember { mutableStateOf<List<ArticleItem>>(emptyList()) }
    var isLoadingTrending by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoadingTrending = true
        try {
            val firestore = FirebaseFirestore.getInstance()
            val usersDoc = firestore.collection("users")
                .orderBy("followersCount", Query.Direction.DESCENDING)
                .limit(5).get().await()
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
                    topGames = (data["topGames"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList(),
                    topSoundtracks = (data["topSoundtracks"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()
                )
            }
            val articlesDoc = firestore.collection("articles")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5).get().await()
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
                            topGames = (data["topGames"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList(),
                            topSoundtracks = (data["topSoundtracks"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()
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

            // Header
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
                        text = "Search users, magazines, albums & articles",
                        fontFamily = NunitoFontFamily,
                        color = ScrapbookDark.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Search bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScrapbookCream)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
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
                                start = 16.dp, end = 16.dp, bottom = 24.dp
                            )
                        ) {
                            if (trendingUsers.isNotEmpty()) {
                                item {
                                    ScrapbookDiscoverHeader(title = "TRENDING USERS", emoji = "🔥")
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

                            if (recentArticles.isNotEmpty()) {
                                item {
                                    ScrapbookDiscoverHeader(title = "RECENT ARTICLES", emoji = "📝")
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                                items(recentArticles.take(3), key = { "trend_art_${it.id}" }) { article ->
                                    ScrapbookTrendingArticleCard(article = article)
                                }
                            }

                            item {
                                ScrapbookDiscoverHeader(title = "FEATURED MAGAZINES", emoji = "📰")
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    items(sampleMagazineCovers.take(5), key = { "trend_mag_${it.id}" }) { mag ->
                                        ScrapbookTrendingMagazineCard(magazine = mag)
                                    }
                                }
                            }

                            item {
                                ScrapbookDiscoverHeader(title = "FEATURED ALBUMS", emoji = "🎵")
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    items(sampleAlbums.take(5), key = { "trend_alb_${it.id}" }) { album ->
                                        ScrapbookTrendingAlbumCard(album = album)
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
                                    Text(
                                        text = "USERS",
                                        fontFamily = BangersFontFamily,
                                        color = ScrapbookDark,
                                        fontSize = 20.sp
                                    )
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
                                        Text(
                                            text = "${realUsers.size}",
                                            fontFamily = BangersFontFamily,
                                            color = ScrapbookDark,
                                            fontSize = 14.sp
                                        )
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
                                            Text(
                                                text = category.label,
                                                fontFamily = BangersFontFamily,
                                                color = ScrapbookDark,
                                                fontSize = 20.sp
                                            )
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
                                                Text(
                                                    text = "${categoryResults.size}",
                                                    fontFamily = BangersFontFamily,
                                                    color = ScrapbookDark,
                                                    fontSize = 14.sp
                                                )
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

// --- Scrapbook Discover Section Header ---
@Composable
fun ScrapbookDiscoverHeader(title: String, emoji: String) {
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
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Divider(
            modifier = Modifier.weight(1f),
            color = ScrapbookBorder.copy(alpha = 0.2f),
            thickness = 2.dp
        )
    }
}

// --- Scrapbook Trending User Card ---
@Composable
fun ScrapbookTrendingUserCard(
    user: UserProfileData,
    isFollowing: Boolean,
    onTap: () -> Unit,
    onFollowClick: () -> Unit
) {
    Box(modifier = Modifier.width(100.dp)) {
        ScrapbookCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTap() },
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
                        .background(
                            if (isFollowing) ScrapbookPaper else ScrapbookDark
                        )
                        .border(
                            2.dp,
                            ScrapbookBorder,
                            RoundedCornerShape(6.dp)
                        )
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

// --- Scrapbook Trending Article Card ---
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
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                    article.author?.let { author ->
                        Text(
                            text = "by $author",
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ScrapbookYellow)
                        .border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "ARTICLE",
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// --- Scrapbook Trending Magazine Card ---
@Composable
fun ScrapbookTrendingMagazineCard(magazine: MagazineCover) {
    Box(modifier = Modifier.width(90.dp)) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 8.dp,
            shadowOffset = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ScrapbookPaper)
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
                        ) { Text("📰", fontSize = 28.sp) }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = magazine.title,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = ScrapbookTextDark,
                    fontSize = 9.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// --- Scrapbook Trending Album Card ---
@Composable
fun ScrapbookTrendingAlbumCard(album: Album) {
    Box(modifier = Modifier.width(90.dp)) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 8.dp,
            shadowOffset = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ScrapbookPaper),
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
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = ScrapbookTextDark,
                    fontSize = 9.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = album.artist,
                    fontFamily = NunitoFontFamily,
                    color = ScrapbookTextMuted,
                    fontSize = 8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// --- Scrapbook User Card in search ---
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
        ScrapbookCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTap() },
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 12.dp,
            shadowOffset = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
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
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
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
                        text = user.userHandle,
                        fontFamily = NunitoFontFamily,
                        color = ScrapbookTextMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (user.bio.isNotBlank()) {
                        Text(
                            text = user.bio,
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (!isCurrentUser) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isFollowing) ScrapbookPaper else ScrapbookDark
                            )
                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                            .clickable { onFollowClick() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isFollowing) "FOLLOWING" else "FOLLOW",
                            fontFamily = BangersFontFamily,
                            fontSize = 13.sp,
                            color = if (isFollowing) ScrapbookDark else ScrapbookYellow
                        )
                    }
                }
            }
        }
    }
}

// --- Scrapbook Result Card ---
@Composable
fun ScrapbookDiscoverResultCard(
    result: DiscoverResult,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.padding(vertical = 4.dp)) {
        ScrapbookCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { },
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
                        .size(8.dp)
                        .background(ScrapbookDark, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.title,
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = result.subtitle,
                        fontFamily = NunitoFontFamily,
                        color = ScrapbookTextMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ScrapbookYellow)
                        .border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = result.category.label,
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// Keep old composable names for backward compatibility
@Composable
fun TrendingSectionHeader(title: String, color: Color) {
    ScrapbookDiscoverHeader(title = title, emoji = "🔥")
}

@Composable
fun TrendingUserCard(
    user: UserProfileData,
    isFollowing: Boolean,
    onTap: () -> Unit,
    onFollowClick: () -> Unit
) {
    ScrapbookTrendingUserCard(
        user = user,
        isFollowing = isFollowing,
        onTap = onTap,
        onFollowClick = onFollowClick
    )
}

@Composable
fun TrendingArticleCard(article: ArticleItem) {
    ScrapbookTrendingArticleCard(article = article)
}

@Composable
fun TrendingMagazineCard(magazine: MagazineCover) {
    ScrapbookTrendingMagazineCard(magazine = magazine)
}

@Composable
fun TrendingAlbumCard(album: Album) {
    ScrapbookTrendingAlbumCard(album = album)
}

@Composable
fun DiscoverUserCard(
    user: UserProfileData,
    isFollowing: Boolean,
    isCurrentUser: Boolean,
    onFollowClick: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScrapbookDiscoverUserCard(
        user = user,
        isFollowing = isFollowing,
        isCurrentUser = isCurrentUser,
        onFollowClick = onFollowClick,
        onTap = onTap,
        modifier = modifier
    )
}

@Composable
fun DiscoverResultCard(result: DiscoverResult, modifier: Modifier = Modifier) {
    ScrapbookDiscoverResultCard(result = result, modifier = modifier)
}
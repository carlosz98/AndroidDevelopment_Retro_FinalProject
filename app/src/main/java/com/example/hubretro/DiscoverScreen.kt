package com.example.hubretro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
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
                    .filter { it.uid != currentUser?.uid } // exclude self
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
            !hasSearched -> DiscoverHints()

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

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // --- Real Users Section ---
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

                    // --- Local Content Sections ---
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
        // Avatar
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

// --- Hints ---
@Composable
fun DiscoverHints() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "WHAT CAN YOU FIND?",
            style = TextStyle(
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite.copy(alpha = 0.5f),
                fontSize = 11.sp,
                letterSpacing = 2.sp
            ),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        listOf(
            Triple("👤", "USERS", "Find and follow other retro enthusiasts"),
            Triple("📰", "MAGAZINES", "Browse classic gaming & tech magazines"),
            Triple("🎵", "ALBUMS", "Discover retro game soundtracks"),
            Triple("📝", "ARTICLES", "Read articles by the community")
        ).forEach { (emoji, title, desc) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RetroDarkPurple.copy(alpha = 0.5f))
                    .border(
                        BorderStroke(1.dp, RetroTextOffWhite.copy(alpha = 0.1f)),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = emoji, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        style = TextStyle(
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = desc,
                        style = TextStyle(
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}
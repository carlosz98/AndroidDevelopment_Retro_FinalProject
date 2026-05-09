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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

enum class FollowListType { FOLLOWERS, FOLLOWING }

@Composable
fun FollowListScreen(
    listType: FollowListType,
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val followingList by authViewModel.followingList.collectAsState()
    val followersList by authViewModel.followersList.collectAsState()
    val followingUids by authViewModel.followingUids.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }
    var searchVisible by remember { mutableStateOf(false) }
    var showFindPeople by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<UserProfileData?>(null) }

    val fullList = if (listType == FollowListType.FOLLOWING) followingList else followersList
    val filteredList = remember(searchQuery, fullList) {
        if (searchQuery.isBlank()) fullList
        else fullList.filter {
            it.username.contains(searchQuery, ignoreCase = true) ||
                    it.userHandle.contains(searchQuery, ignoreCase = true)
        }
    }
    val title = if (listType == FollowListType.FOLLOWING) "FOLLOWING" else "FOLLOWERS"

    if (selectedUser != null) {
        UserProfileViewScreen(
            user = selectedUser!!,
            authViewModel = authViewModel,
            onBack = { selectedUser = null }
        )
        return
    }

    if (showFindPeople) {
        FindPeopleScreen(
            authViewModel = authViewModel,
            onBack = { showFindPeople = false },
            onUserTap = { selectedUser = it }
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
                    .padding(top = 16.dp, bottom = 12.dp, start = 4.dp, end = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ScrapbookDark
                        )
                    }
                    Text(
                        text = title,
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 28.sp,
                        letterSpacing = 2.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    IconButton(onClick = { showFindPeople = true }) {
                        Icon(
                            Icons.Filled.PersonAdd,
                            contentDescription = "Find people",
                            tint = ScrapbookDark
                        )
                    }
                    IconButton(onClick = {
                        searchVisible = !searchVisible
                        if (!searchVisible) {
                            searchQuery = ""
                            focusManager.clearFocus()
                        }
                    }) {
                        Icon(
                            imageVector = if (searchVisible) Icons.Filled.Close
                            else Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = ScrapbookDark
                        )
                    }
                }
            }

            // Count subtitle
            Text(
                text = "${fullList.size} ${title.lowercase()}",
                fontFamily = NunitoFontFamily,
                color = ScrapbookTextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )

            // Search bar
            AnimatedVisibility(
                visible = searchVisible,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search by username or handle...",
                            fontFamily = NunitoFontFamily,
                            fontSize = 14.sp,
                            color = ScrapbookTextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = ScrapbookDark,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
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
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ScrapbookCream)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Empty state
            if (fullList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text("👤", fontSize = 56.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (listType == FollowListType.FOLLOWING)
                                "You're not following anyone yet"
                            else "No followers yet",
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(ScrapbookDark)
                                .border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp))
                                .clickable { showFindPeople = true }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "FIND PEOPLE →",
                                fontFamily = BangersFontFamily,
                                color = ScrapbookYellow,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            } else if (filteredList.isEmpty() && searchQuery.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No results for \"$searchQuery\"",
                        fontFamily = NunitoFontFamily,
                        color = ScrapbookTextMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp,
                        top = 8.dp, bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.uid }) { user ->
                        UserListCard(
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
            }
        }
    }
}

// --- Find People Screen ---
@Composable
fun FindPeopleScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onUserTap: (UserProfileData) -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val followingUids by authViewModel.followingUids.collectAsState()
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<UserProfileData>>(emptyList()) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            delay(600)
            isSearching = true
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
                results = (byUsername.documents + byHandle.documents)
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
            } catch (e: Exception) {
                results = emptyList()
            } finally {
                isSearching = false
            }
        } else {
            results = emptyList()
        }
    }

    Box(
        modifier = Modifier
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
                    .padding(top = 16.dp, bottom = 12.dp, start = 4.dp, end = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ScrapbookDark
                        )
                    }
                    Text(
                        text = "FIND PEOPLE",
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 28.sp,
                        letterSpacing = 2.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.size(48.dp))
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
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search by username or @handle...",
                            fontFamily = NunitoFontFamily,
                            fontSize = 14.sp,
                            color = ScrapbookTextMuted
                        )
                    },
                    leadingIcon = {
                        if (isSearching) {
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
                            IconButton(onClick = { searchQuery = "" }) {
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
                searchQuery.length < 2 -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text("🔍", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Type at least 2 characters\nto search for users",
                                fontFamily = NunitoFontFamily,
                                color = ScrapbookTextMuted,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
                results.isEmpty() && !isSearching -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No users found for \"$searchQuery\"",
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp,
                            top = 4.dp, bottom = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(results, key = { it.uid }) { user ->
                            UserListCard(
                                user = user,
                                isFollowing = followingUids.contains(user.uid),
                                isCurrentUser = user.uid == currentUser?.uid,
                                onFollowClick = {
                                    if (followingUids.contains(user.uid))
                                        authViewModel.unfollowUser(user.uid)
                                    else
                                        authViewModel.followUser(user.uid)
                                },
                                onTap = { onUserTap(user) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- User List Card ---
@Composable
fun UserListCard(
    user: UserProfileData,
    isFollowing: Boolean,
    isCurrentUser: Boolean,
    onFollowClick: () -> Unit,
    onTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
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
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = user.userHandle,
                        fontFamily = NunitoFontFamily,
                        color = ScrapbookTextMuted,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (user.bio.isNotBlank()) {
                        Text(
                            text = user.bio,
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (!isCurrentUser) {
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
                            fontSize = 14.sp,
                            color = if (isFollowing) ScrapbookDark else ScrapbookYellow
                        )
                    }
                }
            }
        }
    }
}
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
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

enum class FollowListType {
    FOLLOWERS, FOLLOWING
}

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
    val titleColor = if (listType == FollowListType.FOLLOWING) VaporwaveCyan else VaporwavePink

    // Show user profile
    if (selectedUser != null) {
        UserProfileViewScreen(
            user = selectedUser!!,
            authViewModel = authViewModel,
            onBack = { selectedUser = null }
        )
        return
    }

    // Show Find People overlay
    if (showFindPeople) {
        FindPeopleScreen(
            authViewModel = authViewModel,
            onBack = { showFindPeople = false },
            onUserTap = { selectedUser = it }
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // --- Header ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Go back",
                    tint = RetroTextOffWhite
                )
            }

            Text(
                text = title,
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(
                        color = titleColor.copy(alpha = 0.8f),
                        offset = Offset(3f, 3f),
                        blurRadius = 6f
                    )
                ),
                modifier = Modifier.weight(1f)
            )

            Row {
                // Find People button
                IconButton(onClick = { showFindPeople = true }) {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = "Find people",
                        tint = VaporwavePink
                    )
                }
                // Search toggle
                IconButton(
                    onClick = {
                        searchVisible = !searchVisible
                        if (!searchVisible) {
                            searchQuery = ""
                            focusManager.clearFocus()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (searchVisible) Icons.Filled.Close else Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = if (searchVisible) titleColor else RetroTextOffWhite
                    )
                }
            }
        }

        Text(
            text = "${fullList.size} ${title.lowercase()}",
            style = TextStyle(
                fontFamily = RetroFontFamily,
                color = titleColor.copy(alpha = 0.7f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- Search bar ---
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
                        fontFamily = RetroFontFamily,
                        fontSize = 12.sp,
                        color = RetroTextOffWhite.copy(alpha = 0.4f)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = titleColor,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Clear",
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
                    focusedBorderColor = titleColor,
                    unfocusedBorderColor = RetroTextOffWhite.copy(alpha = 0.3f),
                    focusedContainerColor = RetroDarkPurple.copy(alpha = 0.8f),
                    unfocusedContainerColor = RetroDarkPurple.copy(alpha = 0.8f),
                    cursorColor = titleColor
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }

        // --- Empty state ---
        if (fullList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = RetroTextOffWhite.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (listType == FollowListType.FOLLOWING)
                            "You're not following anyone yet"
                        else "No followers yet",
                        style = TextStyle(
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite.copy(alpha = 0.5f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showFindPeople = true }) {
                        Text(
                            text = "FIND PEOPLE →",
                            fontFamily = RetroFontFamily,
                            color = VaporwavePink,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else if (filteredList.isEmpty() && searchQuery.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No results for \"$searchQuery\"",
                    style = TextStyle(
                        fontFamily = RetroFontFamily,
                        color = RetroTextOffWhite.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredList, key = { it.uid }) { user ->
                    UserListCard(
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
                    .limit(10)
                    .get()
                    .await()
                val byHandle = firestore.collection("users")
                    .whereGreaterThanOrEqualTo("userHandle", "@${searchQuery.lowercase()}")
                    .whereLessThanOrEqualTo("userHandle", "@${searchQuery.lowercase()}\uf8ff")
                    .limit(10)
                    .get()
                    .await()

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
                    }
                    .filter { it.uid != currentUser?.uid }
            } catch (e: Exception) {
                results = emptyList()
            } finally {
                isSearching = false
            }
        } else {
            results = emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = RetroTextOffWhite
                )
            }
            Text(
                text = "FIND PEOPLE",
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = VaporwavePink.copy(alpha = 0.8f),
                        offset = Offset(3f, 3f),
                        blurRadius = 6f
                    )
                ),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    "Search by username or @handle...",
                    fontFamily = RetroFontFamily,
                    fontSize = 12.sp,
                    color = RetroTextOffWhite.copy(alpha = 0.4f)
                )
            },
            leadingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(
                        color = VaporwavePink,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(2.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = VaporwavePink,
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
                focusedBorderColor = VaporwavePink,
                unfocusedBorderColor = RetroTextOffWhite.copy(alpha = 0.3f),
                focusedContainerColor = RetroDarkPurple.copy(alpha = 0.8f),
                unfocusedContainerColor = RetroDarkPurple.copy(alpha = 0.8f),
                cursorColor = VaporwavePink
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            searchQuery.length < 2 -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Type at least 2 characters\nto search for users",
                        style = TextStyle(
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite.copy(alpha = 0.4f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    )
                }
            }

            results.isEmpty() && !isSearching -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No users found for \"$searchQuery\"",
                        style = TextStyle(
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite.copy(alpha = 0.5f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(results, key = { it.uid }) { user ->
                        UserListCard(
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
                            onTap = { onUserTap(user) }
                        )
                    }
                }
            }
        }
    }
}

// --- User List Card with tap support ---
@Composable
fun UserListCard(
    user: UserProfileData,
    isFollowing: Boolean,
    isCurrentUser: Boolean,
    onFollowClick: () -> Unit,
    onTap: () -> Unit = {},
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
                .border(2.dp, VaporwavePink.copy(alpha = 0.4f), CircleShape),
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
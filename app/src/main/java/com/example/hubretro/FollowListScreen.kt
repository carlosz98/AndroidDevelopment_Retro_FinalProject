package com.example.hubretro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hubretro.ui.theme.*

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

    // Pick the right list based on type
    val fullList = if (listType == FollowListType.FOLLOWING) followingList else followersList

    // Filter by search query
    val filteredList = remember(searchQuery, fullList) {
        if (searchQuery.isBlank()) fullList
        else fullList.filter {
            it.username.contains(searchQuery, ignoreCase = true) ||
                    it.userHandle.contains(searchQuery, ignoreCase = true)
        }
    }

    val title = if (listType == FollowListType.FOLLOWING) "FOLLOWING" else "FOLLOWERS"
    val titleColor = if (listType == FollowListType.FOLLOWING) VaporwaveCyan else VaporwavePink

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // --- Header with back button ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back button
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Go back",
                    tint = RetroTextOffWhite
                )
            }

            // Title
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

        // --- Count badge ---
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

        // --- Animated search bar ---
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
                keyboardActions = KeyboardActions(
                    onSearch = { focusManager.clearFocus() }
                ),
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
                        else
                            "No followers yet",
                        style = TextStyle(
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite.copy(alpha = 0.5f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Use Discover to find retro enthusiasts!",
                        style = TextStyle(
                            fontFamily = RetroFontFamily,
                            color = titleColor.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }

        // --- No search results ---
        else if (filteredList.isEmpty() && searchQuery.isNotBlank()) {
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
        }

        // --- User list ---
        else {
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
                        }
                    )
                }
            }
        }
    }
}

// --- Individual user card ---
@Composable
fun UserListCard(
    user: UserProfileData,
    isFollowing: Boolean,
    isCurrentUser: Boolean,
    onFollowClick: () -> Unit,
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
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar placeholder
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isFollowing) VaporwaveCyan.copy(alpha = 0.2f)
                    else RetroTextOffWhite.copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = if (isFollowing) VaporwaveCyan else RetroTextOffWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // User info
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

        // Follow/Unfollow button — hidden for current user
        if (!isCurrentUser) {
            Button(
                onClick = onFollowClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing)
                        Color.Transparent
                    else
                        VaporwavePink
                ),
                border = if (isFollowing)
                    BorderStroke(1.dp, VaporwaveCyan.copy(alpha = 0.7f))
                else null,
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
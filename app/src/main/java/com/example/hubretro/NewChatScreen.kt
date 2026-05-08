package com.example.hubretro

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
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

@Composable
fun NewChatScreen(
    chatViewModel: ChatViewModel,
    authViewModel: AuthViewModel,
    onChatCreated: (String) -> Unit,
    onBack: () -> Unit
) {
    val profile by authViewModel.userProfile.collectAsState()
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserProfileData>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var selectedUsers by remember { mutableStateOf<List<UserProfileData>>(emptyList()) }
    var groupName by remember { mutableStateOf("") }
    var showGroupNameField by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }

    val isGroup = selectedUsers.size > 1

    // Debounced search
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            delay(500)
            isSearching = true
            try {
                val db = FirebaseFirestore.getInstance()
                val currentUid = chatViewModel.currentUid
                val snap = db.collection("users")
                    .whereGreaterThanOrEqualTo("username", searchQuery.lowercase())
                    .whereLessThanOrEqualTo("username", searchQuery.lowercase() + "\uf8ff")
                    .limit(15).get().await()
                searchResults = snap.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    if (doc.id == currentUid) return@mapNotNull null
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
                        topGames = emptyList(),
                        topSoundtracks = emptyList()
                    )
                }
            } catch (e: Exception) {
                searchResults = emptyList()
            } finally {
                isSearching = false
            }
        } else {
            searchResults = emptyList()
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
                        text = "NEW CHAT",
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 28.sp,
                        letterSpacing = 2.sp,
                        modifier = Modifier.weight(1f)
                    )
                    // Create button — shown when users selected
                    if (selectedUsers.isNotEmpty() && (!isGroup || groupName.isNotBlank())) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ScrapbookDark)
                                .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                                .clickable(enabled = !isCreating) {
                                    if (isGroup && groupName.isBlank()) {
                                        showGroupNameField = true
                                        return@clickable
                                    }
                                    isCreating = true
                                    if (isGroup) {
                                        chatViewModel.createGroupChat(
                                            name = groupName,
                                            members = selectedUsers,
                                            myProfile = profile,
                                            onResult = { chatId -> onChatCreated(chatId) }
                                        )
                                    } else {
                                        chatViewModel.getOrCreateDm(
                                            otherUser = selectedUsers.first(),
                                            myProfile = profile,
                                            onResult = { chatId -> onChatCreated(chatId) }
                                        )
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            if (isCreating) {
                                CircularProgressIndicator(
                                    color = ScrapbookYellow,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (isGroup) "CREATE GROUP" else "START CHAT",
                                    fontFamily = BangersFontFamily,
                                    color = ScrapbookYellow,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Selected users chips
            if (selectedUsers.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ScrapbookPaper)
                        .border(BorderStroke(1.dp, ScrapbookBorder.copy(alpha = 0.3f)))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedUsers.forEach { user ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(ScrapbookYellow)
                                .border(2.dp, ScrapbookBorder, RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = user.username,
                                    fontFamily = BangersFontFamily,
                                    color = ScrapbookDark,
                                    fontSize = 13.sp
                                )
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Remove",
                                    tint = ScrapbookDark,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            selectedUsers = selectedUsers.filter { it.uid != user.uid }
                                        }
                                )
                            }
                        }
                    }
                }
            }

            // Group name field (shown when 2+ users selected)
            if (selectedUsers.size >= 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ScrapbookCream)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        placeholder = {
                            Text(
                                "Group name (required for groups)...",
                                fontFamily = NunitoFontFamily,
                                color = ScrapbookTextMuted,
                                fontSize = 13.sp
                            )
                        },
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
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Search field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScrapbookCream)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search users by username...",
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                color = ScrapbookDark,
                                modifier = Modifier.size(20.dp),
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

            // Hint
            if (searchQuery.length < 2 && selectedUsers.isEmpty()) {
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
                            text = "Search for users to start a DM\nor select multiple for a group chat",
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Search results
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = 4.dp, bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults, key = { it.uid }) { user ->
                    val isSelected = selectedUsers.any { it.uid == user.uid }
                    Box(modifier = Modifier.padding(vertical = 2.dp)) {
                        ScrapbookCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedUsers = if (isSelected) {
                                        selectedUsers.filter { it.uid != user.uid }
                                    } else {
                                        selectedUsers + user
                                    }
                                },
                            backgroundColor = if (isSelected) ScrapbookYellow.copy(alpha = 0.3f)
                            else ScrapbookCardWhite,
                            cornerRadius = 12.dp,
                            shadowOffset = 3.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                                        Icon(
                                            Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = ScrapbookDark.copy(alpha = 0.4f),
                                            modifier = Modifier.size(22.dp)
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
                                }
                                // Checkmark when selected
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(ScrapbookDark)
                                            .border(2.dp, ScrapbookBorder, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = ScrapbookYellow,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
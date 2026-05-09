package com.example.hubretro

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import kotlinx.coroutines.delay

data class ProfileSetupData(
    val username: String = "",
    val profilePictureUri: Uri? = null,
    val bannerUri: Uri? = null,
    val selectedGames: List<IGDBGame> = emptyList(),
    val selectedSoundtracks: List<IGDBSoundtrack> = emptyList(),
    val psnUsername: String = "",
    val xboxUsername: String = "",
    val steamUsername: String = "",
    val nintendoUsername: String = ""
)

@Composable
fun ProfileSetupScreen(
    authViewModel: AuthViewModel = viewModel(),
    onSetupComplete: () -> Unit
) {
    var currentStep by remember { mutableStateOf(1) }
    var setupData by remember { mutableStateOf(ProfileSetupData()) }

    val stepLabels = listOf("USERNAME", "PHOTOS", "GAMES", "MUSIC", "PLATFORMS")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScrapbookCream)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ✅ Yellow header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScrapbookYellow)
                    .border(BorderStroke(2.dp, ScrapbookBorder))
                    .padding(top = 48.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SET UP PROFILE",
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 32.sp,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Step indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(1, 2, 3, 4, 5).forEachIndexed { index, step ->
                            Box(
                                modifier = Modifier
                                    .size(if (currentStep == step) 36.dp else 28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (currentStep >= step) ScrapbookDark
                                        else ScrapbookDark.copy(alpha = 0.2f)
                                    )
                                    .border(2.dp, ScrapbookBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentStep > step) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = ScrapbookYellow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Text(
                                        text = step.toString(),
                                        fontFamily = BangersFontFamily,
                                        color = if (currentStep >= step) ScrapbookYellow
                                        else ScrapbookDark,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            if (index < 4) {
                                Divider(
                                    modifier = Modifier.width(24.dp),
                                    color = if (currentStep > step) ScrapbookDark
                                    else ScrapbookDark.copy(alpha = 0.2f),
                                    thickness = 2.dp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Step labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        stepLabels.forEachIndexed { index, label ->
                            Text(
                                text = label,
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (currentStep == index + 1) ScrapbookDark
                                else ScrapbookDark.copy(alpha = 0.4f),
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Step content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "setup_step_transition",
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    1 -> UsernameStep(
                        authViewModel = authViewModel,
                        initialUsername = setupData.username,
                        onNext = { username ->
                            setupData = setupData.copy(username = username)
                            currentStep = 2
                        }
                    )
                    2 -> ProfilePhotoStep(
                        initialProfileUri = setupData.profilePictureUri,
                        initialBannerUri = setupData.bannerUri,
                        onNext = { profileUri, bannerUri ->
                            setupData = setupData.copy(
                                profilePictureUri = profileUri,
                                bannerUri = bannerUri
                            )
                            currentStep = 3
                        },
                        onBack = { currentStep = 1 }
                    )
                    3 -> TopGamesStep(
                        selectedGames = setupData.selectedGames,
                        onNext = { games ->
                            setupData = setupData.copy(selectedGames = games)
                            currentStep = 4
                        },
                        onBack = { currentStep = 2 }
                    )
                    4 -> TopSoundtracksStep(
                        selectedSoundtracks = setupData.selectedSoundtracks,
                        onNext = { soundtracks ->
                            setupData = setupData.copy(selectedSoundtracks = soundtracks)
                            currentStep = 5
                        },
                        onBack = { currentStep = 3 }
                    )
                    5 -> GamingPlatformsStep(
                        initialPsn = setupData.psnUsername,
                        initialXbox = setupData.xboxUsername,
                        initialSteam = setupData.steamUsername,
                        initialNintendo = setupData.nintendoUsername,
                        onComplete = { psn, xbox, steam, nintendo ->
                            setupData = setupData.copy(
                                psnUsername = psn,
                                xboxUsername = xbox,
                                steamUsername = steam,
                                nintendoUsername = nintendo
                            )
                            authViewModel.completeProfileSetup(setupData)
                            onSetupComplete()
                        },
                        onBack = { currentStep = 4 }
                    )
                }
            }
        }
    }
}

// --- Step 1: Username ---
@Composable
fun UsernameStep(
    authViewModel: AuthViewModel,
    initialUsername: String,
    onNext: (String) -> Unit
) {
    var username by remember { mutableStateOf(initialUsername) }
    var isChecking by remember { mutableStateOf(false) }
    var isAvailable by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(username) {
        if (username.length >= 3) {
            delay(600)
            isChecking = true
            isAvailable = authViewModel.checkUsernameAvailable(username)
            isChecking = false
        } else {
            isAvailable = null
        }
    }

    SetupStepContainer(
        title = "CHOOSE YOUR USERNAME",
        subtitle = "Pick a unique name for your RetroHub profile"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it.lowercase().replace(" ", "")
                    isAvailable = null
                },
                placeholder = {
                    Text(
                        "retro_gamer_99",
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        color = ScrapbookTextMuted
                    )
                },
                leadingIcon = {
                    Text(
                        "@",
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                trailingIcon = {
                    when {
                        isChecking -> CircularProgressIndicator(
                            color = ScrapbookYellowDark,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        isAvailable == true -> Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Available",
                            tint = ScrapbookGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        isAvailable == false -> Icon(
                            Icons.Filled.Close,
                            contentDescription = "Taken",
                            tint = ScrapbookRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = NunitoFontFamily,
                    fontSize = 16.sp,
                    color = ScrapbookTextDark
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = when {
                        isAvailable == true -> ScrapbookGreen
                        isAvailable == false -> ScrapbookRed
                        else -> ScrapbookDark
                    },
                    unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f),
                    focusedContainerColor = ScrapbookCardWhite,
                    unfocusedContainerColor = ScrapbookCardWhite,
                    cursorColor = ScrapbookDark,
                    focusedTextColor = ScrapbookTextDark,
                    unfocusedTextColor = ScrapbookTextDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            when {
                username.length in 1..2 -> Text(
                    "Username must be at least 3 characters",
                    fontFamily = NunitoFontFamily,
                    color = ScrapbookTextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                isAvailable == true -> Text(
                    "✓ @$username is available!",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = ScrapbookGreen,
                    fontSize = 13.sp
                )
                isAvailable == false -> Text(
                    "✗ @$username is already taken",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = ScrapbookRed,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isAvailable == true) ScrapbookDark
                        else ScrapbookDark.copy(alpha = 0.3f)
                    )
                    .border(2.dp, ScrapbookBorder, RoundedCornerShape(12.dp))
                    .clickable(enabled = isAvailable == true) { onNext(username) }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "NEXT →",
                    fontFamily = BangersFontFamily,
                    fontSize = 20.sp,
                    color = ScrapbookYellow
                )
            }
        }
    }
}

// --- Step 2: Profile Photos ---
@Composable
fun ProfilePhotoStep(
    initialProfileUri: Uri?,
    initialBannerUri: Uri?,
    onNext: (profileUri: Uri?, bannerUri: Uri?) -> Unit,
    onBack: () -> Unit
) {
    var profileUri by remember { mutableStateOf(initialProfileUri) }
    var bannerUri by remember { mutableStateOf(initialBannerUri) }

    val profileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> profileUri = uri }
    val bannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> bannerUri = uri }

    SetupStepContainer(
        title = "PROFILE PHOTOS",
        subtitle = "Set your profile picture and banner"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "BANNER",
                fontFamily = BangersFontFamily,
                color = ScrapbookDark,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ScrapbookPaper)
                    .border(2.dp, ScrapbookBorder, RoundedCornerShape(12.dp))
                    .clickable { bannerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (bannerUri != null) {
                    AsyncImage(
                        model = bannerUri,  // ✅ uses bannerUri correctly
                        contentDescription = "Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.AddPhotoAlternate,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.AddPhotoAlternate,
                            contentDescription = null,
                            tint = ScrapbookDark.copy(alpha = 0.3f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "TAP TO ADD BANNER",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark.copy(alpha = 0.4f),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "PROFILE PICTURE",
                fontFamily = BangersFontFamily,
                color = ScrapbookDark,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ScrapbookPaper)
                    .border(2.dp, ScrapbookBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Just the circle profile pic — no banner background
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(ScrapbookCardWhite)
                        .border(3.dp, ScrapbookBorder, CircleShape)
                        .clickable { profileLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (profileUri != null) {
                        AsyncImage(
                            model = profileUri,
                            contentDescription = "Profile picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                tint = ScrapbookDark.copy(alpha = 0.3f),
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                "TAP",
                                fontFamily = BangersFontFamily,
                                color = ScrapbookDark.copy(alpha = 0.4f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ScrapbookPaper)
                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                        .clickable { bannerLauncher.launch("image/*") }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (bannerUri != null) "CHANGE BANNER" else "ADD BANNER",
                        fontFamily = BangersFontFamily,
                        fontSize = 13.sp,
                        color = ScrapbookDark,
                        textAlign = TextAlign.Center
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ScrapbookPaper)
                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                        .clickable { profileLauncher.launch("image/*") }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (profileUri != null) "CHANGE PHOTO" else "ADD PHOTO",
                        fontFamily = BangersFontFamily,
                        fontSize = 13.sp,
                        color = ScrapbookDark,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ScrapbookPaper)
                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(12.dp))
                        .clickable { onBack() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "← BACK",
                        fontFamily = BangersFontFamily,
                        fontSize = 18.sp,
                        color = ScrapbookDark
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ScrapbookDark)
                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(12.dp))
                        .clickable { onNext(profileUri, bannerUri) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "NEXT →",
                        fontFamily = BangersFontFamily,
                        fontSize = 18.sp,
                        color = ScrapbookYellow
                    )
                }
            }
        }
    }
}

// --- Step 3: Top 6 Games ---
@Composable
fun TopGamesStep(
    selectedGames: List<IGDBGame>,
    onNext: (List<IGDBGame>) -> Unit,
    onBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<IGDBGame>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(selectedGames.toMutableList()) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            delay(600)
            isSearching = true
            searchResults = IGDBRepository.searchGames(searchQuery)
            isSearching = false
        } else {
            searchResults = emptyList()
        }
    }

    SetupStepContainer(
        title = "TOP 6 GAMES",
        subtitle = "Search and pick your 6 favorite games"
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${selected.size}/6 selected",
                fontFamily = BangersFontFamily,
                color = if (selected.size == 6) ScrapbookGreen else ScrapbookDark,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search for a game...",
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        color = ScrapbookTextMuted
                    )
                },
                leadingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            color = ScrapbookYellowDark,
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
                                contentDescription = null,
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
                    cursorColor = ScrapbookDark,
                    focusedTextColor = ScrapbookTextDark,
                    unfocusedTextColor = ScrapbookTextDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (selected.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                ) {
                    items(selected, key = { it.id }) { game ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(3f / 4f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ScrapbookPaper)
                                .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    selected = selected.toMutableList().also { it.remove(game) }
                                }
                        ) {
                            if (game.coverUrl != null) {
                                AsyncImage(
                                    model = game.coverUrl,
                                    contentDescription = game.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.TopEnd
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Remove",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp).padding(2.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults, key = { it.id }) { game ->
                    val isSelected = selected.any { it.id == game.id }
                    GameSearchResultItem(
                        game = game,
                        isSelected = isSelected,
                        onToggle = {
                            if (isSelected) {
                                selected = selected.toMutableList().also { list ->
                                    list.removeAll { it.id == game.id }
                                }
                            } else if (selected.size < 6) {
                                selected = selected.toMutableList().also { it.add(game) }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ScrapbookPaper)
                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(12.dp))
                        .clickable { onBack() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("← BACK", fontFamily = BangersFontFamily, fontSize = 18.sp, color = ScrapbookDark)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selected.size == 6) ScrapbookDark
                            else ScrapbookDark.copy(alpha = 0.3f)
                        )
                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(12.dp))
                        .clickable(enabled = selected.size == 6) { onNext(selected) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("NEXT →", fontFamily = BangersFontFamily, fontSize = 18.sp, color = ScrapbookYellow)
                }
            }
        }
    }
}

// --- Step 4: Top 3 Soundtracks ---
@Composable
fun TopSoundtracksStep(
    selectedSoundtracks: List<IGDBSoundtrack>,
    onNext: (List<IGDBSoundtrack>) -> Unit,
    onBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<IGDBSoundtrack>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(selectedSoundtracks.toMutableList()) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            delay(600)
            isSearching = true
            searchResults = IGDBRepository.searchSoundtracks(searchQuery)
            isSearching = false
        } else {
            searchResults = emptyList()
        }
    }

    SetupStepContainer(
        title = "TOP 3 SOUNDTRACKS",
        subtitle = "Search and pick your 3 favorite game OSTs"
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${selected.size}/3 selected",
                fontFamily = BangersFontFamily,
                color = if (selected.size == 3) ScrapbookGreen else ScrapbookDark,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search for a game soundtrack...",
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        color = ScrapbookTextMuted
                    )
                },
                leadingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            color = ScrapbookYellowDark,
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
                                contentDescription = null,
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
                    cursorColor = ScrapbookDark,
                    focusedTextColor = ScrapbookTextDark,
                    unfocusedTextColor = ScrapbookTextDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Vinyl previews
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(3) { index ->
                    val soundtrack = selected.getOrNull(index)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(
                                if (soundtrack != null) ScrapbookDark
                                else ScrapbookPaper
                            )
                            .border(
                                if (soundtrack != null) 3.dp else 2.dp,
                                ScrapbookBorder,
                                CircleShape
                            )
                            .then(
                                if (soundtrack != null) Modifier.clickable {
                                    selected = selected.toMutableList().also { it.remove(soundtrack) }
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (soundtrack != null) {
                            if (soundtrack.coverUrl != null) {
                                AsyncImage(
                                    model = soundtrack.coverUrl,
                                    contentDescription = soundtrack.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(ScrapbookCardWhite, CircleShape)
                                    .border(2.dp, ScrapbookBorder, CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.TopEnd
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Remove",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp).padding(2.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "${index + 1}",
                                fontFamily = BangersFontFamily,
                                color = ScrapbookDark.copy(alpha = 0.3f),
                                fontSize = 22.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults, key = { it.id }) { soundtrack ->
                    val isSelected = selected.any { it.id == soundtrack.id }
                    SoundtrackSearchResultItem(
                        soundtrack = soundtrack,
                        isSelected = isSelected,
                        onToggle = {
                            if (isSelected) {
                                selected = selected.toMutableList().also { list ->
                                    list.removeAll { it.id == soundtrack.id }
                                }
                            } else if (selected.size < 3) {
                                selected = selected.toMutableList().also { it.add(soundtrack) }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ScrapbookPaper)
                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(12.dp))
                        .clickable { onBack() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("← BACK", fontFamily = BangersFontFamily, fontSize = 18.sp, color = ScrapbookDark)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selected.size == 3) ScrapbookDark
                            else ScrapbookDark.copy(alpha = 0.3f)
                        )
                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(12.dp))
                        .clickable(enabled = selected.size == 3) { onNext(selected) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("NEXT →", fontFamily = BangersFontFamily, fontSize = 18.sp, color = ScrapbookYellow)
                }
            }
        }
    }
}

// --- Step 5: Gaming Platforms ---
@Composable
fun GamingPlatformsStep(
    initialPsn: String,
    initialXbox: String,
    initialSteam: String,
    initialNintendo: String,
    onComplete: (psn: String, xbox: String, steam: String, nintendo: String) -> Unit,
    onBack: () -> Unit
) {
    var psn by remember { mutableStateOf(initialPsn) }
    var xbox by remember { mutableStateOf(initialXbox) }
    var steam by remember { mutableStateOf(initialSteam) }
    var nintendo by remember { mutableStateOf(initialNintendo) }

    SetupStepContainer(
        title = "GAMING PLATFORMS",
        subtitle = "Add your platform usernames so friends can find you"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "All fields are optional — add as many as you like!",
                fontFamily = NunitoFontFamily,
                color = ScrapbookTextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            PlatformSetupField(value = psn, onValueChange = { psn = it }, platform = gamingPlatforms[0])
            PlatformSetupField(value = xbox, onValueChange = { xbox = it }, platform = gamingPlatforms[1])
            PlatformSetupField(value = steam, onValueChange = { steam = it }, platform = gamingPlatforms[2])
            PlatformSetupField(value = nintendo, onValueChange = { nintendo = it }, platform = gamingPlatforms[3])

            val activePlatforms = listOf(
                gamingPlatforms[0] to psn,
                gamingPlatforms[1] to xbox,
                gamingPlatforms[2] to steam,
                gamingPlatforms[3] to nintendo
            ).filter { (_, username) -> username.isNotBlank() }

            if (activePlatforms.isNotEmpty()) {
                Text(
                    text = "PREVIEW",
                    fontFamily = BangersFontFamily,
                    color = ScrapbookDark,
                    fontSize = 16.sp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    activePlatforms.forEach { (platform, username) ->
                        PlatformBubble(platform = platform, username = username)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ScrapbookPaper)
                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(12.dp))
                        .clickable { onBack() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("← BACK", fontFamily = BangersFontFamily, fontSize = 18.sp, color = ScrapbookDark)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ScrapbookDark)
                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            onComplete(psn.trim(), xbox.trim(), steam.trim(), nintendo.trim())
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("FINISH ✓", fontFamily = BangersFontFamily, fontSize = 18.sp, color = ScrapbookYellow)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// --- Platform Setup Field ---
@Composable
fun PlatformSetupField(
    value: String,
    onValueChange: (String) -> Unit,
    platform: GamingPlatform
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ScrapbookDark)
                    .border(2.dp, ScrapbookBorder, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = platform.iconResId),
                    contentDescription = platform.name,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = platform.name.uppercase(),
                fontFamily = BangersFontFamily,
                color = ScrapbookDark,
                fontSize = 16.sp
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    "Your ${platform.name} username",
                    fontFamily = NunitoFontFamily,
                    fontSize = 13.sp,
                    color = ScrapbookTextMuted
                )
            },
            leadingIcon = {
                Text(
                    "@",
                    fontFamily = BangersFontFamily,
                    color = ScrapbookDark,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            },
            trailingIcon = {
                if (value.isNotBlank()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Clear",
                            tint = ScrapbookTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
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
                cursorColor = ScrapbookDark,
                focusedTextColor = ScrapbookTextDark,
                unfocusedTextColor = ScrapbookTextDark
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// --- Game Search Result Item ---
@Composable
fun GameSearchResultItem(
    game: IGDBGame,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        ScrapbookCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() },
            backgroundColor = if (isSelected) ScrapbookYellow.copy(alpha = 0.3f)
            else ScrapbookCardWhite,
            cornerRadius = 10.dp,
            shadowOffset = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ScrapbookPaper)
                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(6.dp))
                ) {
                    if (game.coverUrl != null) {
                        AsyncImage(
                            model = game.coverUrl,
                            contentDescription = game.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = game.name,
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    game.releaseYear?.let { year ->
                        Text(
                            text = year.toString(),
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
                if (isSelected) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Selected",
                        tint = ScrapbookGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// --- Soundtrack Search Result Item ---
@Composable
fun SoundtrackSearchResultItem(
    soundtrack: IGDBSoundtrack,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        ScrapbookCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() },
            backgroundColor = if (isSelected) ScrapbookYellow.copy(alpha = 0.3f)
            else ScrapbookCardWhite,
            cornerRadius = 10.dp,
            shadowOffset = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ScrapbookDark)
                        .border(2.dp, ScrapbookBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (soundtrack.coverUrl != null) {
                        AsyncImage(
                            model = soundtrack.coverUrl,
                            contentDescription = soundtrack.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(ScrapbookCardWhite, CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = soundtrack.name,
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    soundtrack.gameName?.let { gameName ->
                        Text(
                            text = gameName,
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (isSelected) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Selected",
                        tint = ScrapbookGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// --- Reusable setup step container ---
@Composable
fun SetupStepContainer(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            fontFamily = BangersFontFamily,
            color = ScrapbookDark,
            fontSize = 28.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            fontFamily = NunitoFontFamily,
            color = ScrapbookTextMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        content()
    }
}
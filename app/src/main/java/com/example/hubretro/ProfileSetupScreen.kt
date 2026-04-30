package com.example.hubretro

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Data class to store setup progress
data class ProfileSetupData(
    val username: String = "",
    val profilePictureUri: Uri? = null,
    val bannerUri: Uri? = null,
    val selectedGames: List<IGDBGame> = emptyList(),
    val selectedSoundtracks: List<IGDBSoundtrack> = emptyList()
)

@Composable
fun ProfileSetupScreen(
    authViewModel: AuthViewModel = viewModel(),
    onSetupComplete: () -> Unit
) {
    var currentStep by remember { mutableStateOf(1) }
    var setupData by remember { mutableStateOf(ProfileSetupData()) }

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.my_retro_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

        Column(modifier = Modifier.fillMaxSize()) {

            Spacer(modifier = Modifier.height(48.dp))

            // Step indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(1, 2, 3, 4).forEachIndexed { index, step ->
                    Box(
                        modifier = Modifier
                            .size(if (currentStep == step) 36.dp else 28.dp)
                            .background(
                                if (currentStep >= step) VaporwavePink
                                else RetroTextOffWhite.copy(alpha = 0.3f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentStep > step) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = step.toString(),
                                fontFamily = RetroFontFamily,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (index < 3) {
                        Divider(
                            modifier = Modifier.width(40.dp),
                            color = if (currentStep > step)
                                VaporwavePink.copy(alpha = 0.7f)
                            else
                                RetroTextOffWhite.copy(alpha = 0.2f),
                            thickness = 2.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Step labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf("USERNAME", "PHOTOS", "GAMES", "MUSIC").forEach { label ->
                    Text(
                        text = label,
                        fontFamily = RetroFontFamily,
                        color = RetroTextOffWhite.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                        onComplete = { soundtracks ->
                            setupData = setupData.copy(selectedSoundtracks = soundtracks)
                            authViewModel.completeProfileSetup(setupData)
                            onSetupComplete()
                        },
                        onBack = { currentStep = 3 }
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
        title = "CHOOSE YOUR\nUSERNAME",
        subtitle = "Pick a unique name for your RetroHub profile",
        titleColor = VaporwavePink
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
                        fontFamily = RetroFontFamily,
                        fontSize = 13.sp,
                        color = RetroTextOffWhite.copy(alpha = 0.3f)
                    )
                },
                leadingIcon = {
                    Text(
                        "@",
                        fontFamily = RetroFontFamily,
                        color = VaporwavePink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                trailingIcon = {
                    when {
                        isChecking -> CircularProgressIndicator(
                            color = VaporwavePink,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        isAvailable == true -> Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Available",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                        isAvailable == false -> Icon(
                            Icons.Filled.Close,
                            contentDescription = "Taken",
                            tint = SynthwaveOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = RetroFontFamily,
                    fontSize = 16.sp,
                    color = RetroTextOffWhite
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = when {
                        isAvailable == true -> Color(0xFF4CAF50)
                        isAvailable == false -> SynthwaveOrange
                        else -> VaporwavePink
                    },
                    unfocusedBorderColor = RetroTextOffWhite.copy(alpha = 0.3f),
                    focusedContainerColor = Color(0xFF12122A),
                    unfocusedContainerColor = Color(0xFF12122A),
                    cursorColor = VaporwavePink,
                    focusedTextColor = RetroTextOffWhite,
                    unfocusedTextColor = RetroTextOffWhite
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            when {
                username.length in 1..2 -> Text(
                    "Username must be at least 3 characters",
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
                isAvailable == true -> Text(
                    "✓ @$username is available!",
                    fontFamily = RetroFontFamily,
                    color = Color(0xFF4CAF50),
                    fontSize = 11.sp
                )
                isAvailable == false -> Text(
                    "✗ @$username is already taken",
                    fontFamily = RetroFontFamily,
                    color = SynthwaveOrange,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onNext(username) },
                enabled = isAvailable == true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VaporwavePink,
                    disabledContainerColor = RetroTextOffWhite.copy(alpha = 0.2f)
                )
            ) {
                Text(
                    "NEXT →",
                    fontFamily = RetroFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}

// --- Step 2: Profile Photo + Banner ---
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
        contract = ActivityResultContracts.GetContent()
    ) { uri -> profileUri = uri }

    val bannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> bannerUri = uri }

    SetupStepContainer(
        title = "PROFILE\nPHOTOS",
        subtitle = "Set your profile picture and banner",
        titleColor = VaporwavePurple
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- Banner picker ---
            Text(
                text = "BANNER",
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RetroDarkPurple)
                    .border(2.dp, VaporwavePurple.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .clickable { bannerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (bannerUri != null) {
                    AsyncImage(
                        model = bannerUri,
                        contentDescription = "Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Edit overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.AddPhotoAlternate,
                            contentDescription = "Change banner",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.AddPhotoAlternate,
                            contentDescription = null,
                            tint = RetroTextOffWhite.copy(alpha = 0.4f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "TAP TO ADD BANNER",
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite.copy(alpha = 0.4f),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- Profile picture picker ---
            Text(
                text = "PROFILE PICTURE",
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )

            // Preview — shows banner behind avatar just like the real profile
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RetroDarkPurple)
            ) {
                // Mini banner preview
                if (bannerUri != null) {
                    AsyncImage(
                        model = bannerUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                )

                // Profile picture circle
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(RetroDarkPurple)
                        .border(3.dp, VaporwavePurple, CircleShape)
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
                                tint = RetroTextOffWhite.copy(alpha = 0.4f),
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                "TAP",
                                fontFamily = RetroFontFamily,
                                color = RetroTextOffWhite.copy(alpha = 0.4f),
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Buttons row for changing individually
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { bannerLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, VaporwavePurple.copy(alpha = 0.6f))
                ) {
                    Text(
                        if (bannerUri != null) "CHANGE\nBANNER" else "ADD\nBANNER",
                        fontFamily = RetroFontFamily,
                        fontSize = 10.sp,
                        color = VaporwavePurple,
                        textAlign = TextAlign.Center
                    )
                }
                OutlinedButton(
                    onClick = { profileLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, VaporwavePurple)
                ) {
                    Text(
                        if (profileUri != null) "CHANGE\nPHOTO" else "ADD\nPHOTO",
                        fontFamily = RetroFontFamily,
                        fontSize = 10.sp,
                        color = VaporwavePurple,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, RetroTextOffWhite.copy(alpha = 0.4f))
                ) {
                    Text(
                        "← BACK",
                        fontFamily = RetroFontFamily,
                        fontSize = 13.sp,
                        color = RetroTextOffWhite
                    )
                }

                Button(
                    onClick = { onNext(profileUri, bannerUri) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = VaporwavePurple)
                ) {
                    Text(
                        "NEXT →",
                        fontFamily = RetroFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
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
        subtitle = "Search and pick your 6 favorite games",
        titleColor = SynthwaveOrange
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            Text(
                text = "${selected.size}/6 selected",
                fontFamily = RetroFontFamily,
                color = if (selected.size == 6) Color(0xFF4CAF50) else SynthwaveOrange,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search for a game...",
                        fontFamily = RetroFontFamily,
                        fontSize = 12.sp,
                        color = RetroTextOffWhite.copy(alpha = 0.4f)
                    )
                },
                leadingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            color = SynthwaveOrange,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(2.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = SynthwaveOrange,
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
                                tint = RetroTextOffWhite.copy(alpha = 0.5f),
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
                    focusedBorderColor = SynthwaveOrange,
                    unfocusedBorderColor = RetroTextOffWhite.copy(alpha = 0.3f),
                    focusedContainerColor = Color(0xFF12122A),
                    unfocusedContainerColor = Color(0xFF12122A),
                    cursorColor = SynthwaveOrange,
                    focusedTextColor = RetroTextOffWhite,
                    unfocusedTextColor = RetroTextOffWhite
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (selected.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    items(selected, key = { it.id }) { game ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(3f / 4f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(RetroDarkPurple)
                                .border(2.dp, SynthwaveOrange, RoundedCornerShape(8.dp))
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
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(2.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, RetroTextOffWhite.copy(alpha = 0.4f))
                ) {
                    Text(
                        "← BACK",
                        fontFamily = RetroFontFamily,
                        fontSize = 13.sp,
                        color = RetroTextOffWhite
                    )
                }
                Button(
                    onClick = { onNext(selected) },
                    enabled = selected.size == 6,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SynthwaveOrange,
                        disabledContainerColor = RetroTextOffWhite.copy(alpha = 0.2f)
                    )
                ) {
                    Text(
                        "NEXT →",
                        fontFamily = RetroFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// --- Step 4: Top 3 Soundtracks ---
@Composable
fun TopSoundtracksStep(
    selectedSoundtracks: List<IGDBSoundtrack>,
    onComplete: (List<IGDBSoundtrack>) -> Unit,
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
        title = "TOP 3\nSOUNDTRACKS",
        subtitle = "Search and pick your 3 favorite game OSTs",
        titleColor = VaporwaveCyan
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            Text(
                text = "${selected.size}/3 selected",
                fontFamily = RetroFontFamily,
                color = if (selected.size == 3) Color(0xFF4CAF50) else VaporwaveCyan,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search for a game soundtrack...",
                        fontFamily = RetroFontFamily,
                        fontSize = 12.sp,
                        color = RetroTextOffWhite.copy(alpha = 0.4f)
                    )
                },
                leadingIcon = {
                    if (isSearching) {
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
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = null,
                                tint = RetroTextOffWhite.copy(alpha = 0.5f),
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
                    focusedContainerColor = Color(0xFF12122A),
                    unfocusedContainerColor = Color(0xFF12122A),
                    cursorColor = VaporwaveCyan,
                    focusedTextColor = RetroTextOffWhite,
                    unfocusedTextColor = RetroTextOffWhite
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Vinyl previews
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
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
                                if (soundtrack != null) Color(0xFF1A1A1A)
                                else RetroDarkPurple.copy(alpha = 0.5f)
                            )
                            .border(
                                if (soundtrack != null) 3.dp else 2.dp,
                                if (soundtrack != null) VaporwaveCyan
                                else RetroTextOffWhite.copy(alpha = 0.2f),
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
                            // Vinyl center hole
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color.Black, CircleShape)
                                    .border(1.dp, VaporwaveCyan.copy(alpha = 0.5f), CircleShape)
                            )
                            // Remove overlay
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
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, RetroTextOffWhite.copy(alpha = 0.4f))
                ) {
                    Text(
                        "← BACK",
                        fontFamily = RetroFontFamily,
                        fontSize = 13.sp,
                        color = RetroTextOffWhite
                    )
                }
                Button(
                    onClick = { onComplete(selected) },
                    enabled = selected.size == 3,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VaporwaveCyan,
                        disabledContainerColor = RetroTextOffWhite.copy(alpha = 0.2f)
                    )
                ) {
                    Text(
                        "FINISH ✓",
                        fontFamily = RetroFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) SynthwaveOrange.copy(alpha = 0.15f)
                else RetroDarkPurple.copy(alpha = 0.7f)
            )
            .border(
                1.dp,
                if (isSelected) SynthwaveOrange else RetroTextOffWhite.copy(alpha = 0.15f),
                RoundedCornerShape(10.dp)
            )
            .clickable { onToggle() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF2A2A3A))
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
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            game.releaseYear?.let { year ->
                Text(
                    text = year.toString(),
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
        }
        if (isSelected) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Selected",
                tint = SynthwaveOrange,
                modifier = Modifier.size(24.dp)
            )
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) VaporwaveCyan.copy(alpha = 0.15f)
                else RetroDarkPurple.copy(alpha = 0.7f)
            )
            .border(
                1.dp,
                if (isSelected) VaporwaveCyan else RetroTextOffWhite.copy(alpha = 0.15f),
                RoundedCornerShape(10.dp)
            )
            .clickable { onToggle() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A1A1A))
                .border(2.dp, VaporwaveCyan.copy(alpha = 0.5f), CircleShape),
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
                    .background(Color.Black, CircleShape)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = soundtrack.name,
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            soundtrack.gameName?.let { gameName ->
                Text(
                    text = gameName,
                    fontFamily = RetroFontFamily,
                    color = VaporwaveCyan.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (isSelected) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Selected",
                tint = VaporwaveCyan,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// --- Reusable step container ---
@Composable
fun SetupStepContainer(
    title: String,
    subtitle: String,
    titleColor: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = TextStyle(
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                shadow = Shadow(
                    color = titleColor.copy(alpha = 0.8f),
                    offset = Offset(4f, 4f),
                    blurRadius = 8f
                )
            ),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = TextStyle(
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite.copy(alpha = 0.5f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        content()
    }
}
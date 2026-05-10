package com.example.hubretro

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class MarketplaceListing(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val price: String = "",
    val condition: String = "",
    val type: String = "FOR SALE", // FOR SALE, FOR TRADE, WANTED
    val imageUrl: String = "",
    val sellerUid: String = "",
    val sellerUsername: String = "",
    val sellerProfilePicUrl: String = "",
    val timestamp: Long = 0L,
    val platform: String = ""
)

val listingTypes = listOf("ALL", "FOR SALE", "FOR TRADE", "WANTED")
val gameConditions = listOf("MINT", "EXCELLENT", "GOOD", "FAIR", "POOR")
val gamePlatformsList = listOf(
    "ANY", "NES", "SNES", "N64", "GAMEBOY",
    "PS1", "PS2", "PS3", "SEGA", "ARCADE", "PC", "OTHER"
)

@Composable
fun MarketplaceScreen(
    authViewModel: AuthViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val firebaseProfile by authViewModel.userProfile.collectAsState()
    val followingUids by authViewModel.followingUids.collectAsState()

    var listings by remember { mutableStateOf<List<MarketplaceListing>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedType by remember { mutableStateOf("ALL") }
    var showAddListing by remember { mutableStateOf(false) }
    var selectedListing by remember { mutableStateOf<MarketplaceListing?>(null) }
    var selectedSeller by remember { mutableStateOf<UserProfileData?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val docs = FirebaseFirestore.getInstance()
                .collection("marketplace")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get().await()
            listings = docs.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                MarketplaceListing(
                    id = doc.id,
                    title = data["title"] as? String ?: "",
                    description = data["description"] as? String ?: "",
                    price = data["price"] as? String ?: "",
                    condition = data["condition"] as? String ?: "",
                    type = data["type"] as? String ?: "FOR SALE",
                    imageUrl = data["imageUrl"] as? String ?: "",
                    sellerUid = data["sellerUid"] as? String ?: "",
                    sellerUsername = data["sellerUsername"] as? String ?: "",
                    sellerProfilePicUrl = data["sellerProfilePicUrl"] as? String ?: "",
                    timestamp = (data["timestamp"] as? Long) ?: 0L,
                    platform = data["platform"] as? String ?: ""
                )
            }.filter { it.title.isNotBlank() }
        } catch (e: Exception) { } finally {
            isLoading = false
        }
    }

    val filteredListings = remember(listings, selectedType) {
        if (selectedType == "ALL") listings
        else listings.filter { it.type == selectedType }
    }

    // Show seller profile
    if (selectedSeller != null) {
        UserProfileViewScreen(
            user = selectedSeller!!,
            authViewModel = authViewModel,
            onBack = { selectedSeller = null },
            chatViewModel = chatViewModel,
            onOpenChat = { selectedSeller = null }
        )
        return
    }

    // Show listing detail
    if (selectedListing != null) {
        ListingDetailScreen(
            listing = selectedListing!!,
            currentUid = currentUser?.uid ?: "",
            chatViewModel = chatViewModel,
            authViewModel = authViewModel,
            onBack = { selectedListing = null },
            onViewSeller = { uid ->
                // fetch seller profile
                FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .get()
                    .addOnSuccessListener { doc ->
                        val data = doc.data ?: return@addOnSuccessListener
                        selectedSeller = UserProfileData(
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
            }
        )
        return
    }

    // Show add listing
    if (showAddListing) {
        AddListingScreen(
            sellerUid = currentUser?.uid ?: "",
            sellerUsername = firebaseProfile?.username ?: "",
            sellerProfilePicUrl = firebaseProfile?.profilePictureUrl ?: "",
            onDismiss = { showAddListing = false },
            onSaved = { listing ->
                listings = listOf(listing) + listings
                showAddListing = false
            }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🛒 MARKETPLACE",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 28.sp,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Buy, sell & trade retro games",
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookDark.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                    if (currentUser != null && firebaseProfile?.setupComplete == true) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ScrapbookDark)
                                .border(2.dp, ScrapbookBorder, CircleShape)
                                .clickable { showAddListing = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Add listing",
                                tint = ScrapbookYellow,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Type filter
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listingTypes) { type ->
                    val isSelected = selectedType == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                when {
                                    isSelected && type == "FOR SALE" -> ScrapbookGreen
                                    isSelected && type == "FOR TRADE" -> ScrapbookBlue
                                    isSelected && type == "WANTED" -> ScrapbookRed
                                    isSelected -> ScrapbookDark
                                    else -> ScrapbookCardWhite
                                }
                            )
                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(20.dp))
                            .clickable { selectedType = type }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = type,
                            fontFamily = BangersFontFamily,
                            color = if (isSelected) Color.White else ScrapbookDark,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = ScrapbookYellowDark,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                filteredListings.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text("🛒", fontSize = 56.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "NO LISTINGS YET",
                                fontFamily = BangersFontFamily,
                                color = ScrapbookDark,
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (currentUser != null && firebaseProfile?.setupComplete == true)
                                    "Be the first to post a listing!\nTap + to add one."
                                else "Sign in and complete your profile\nto post listings.",
                                fontFamily = NunitoFontFamily,
                                color = ScrapbookTextMuted,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp,
                            top = 4.dp, bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${filteredListings.size} listing${if (filteredListings.size != 1) "s" else ""}",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = ScrapbookTextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        items(filteredListings, key = { it.id }) { listing ->
                            ListingCard(
                                listing = listing,
                                onClick = { selectedListing = listing }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ListingCard(listing: MarketplaceListing, onClick: () -> Unit) {
    val typeColor = when (listing.type) {
        "FOR SALE" -> ScrapbookGreen
        "FOR TRADE" -> ScrapbookBlue
        "WANTED" -> ScrapbookRed
        else -> ScrapbookDark
    }
    Box {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth().clickable { onClick() },
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 14.dp,
            shadowOffset = 4.dp
        ) {
            Row(modifier = Modifier.padding(12.dp)) {
                // Image
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ScrapbookPaper)
                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (listing.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = listing.imageUrl,
                            contentDescription = listing.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("🎮", fontSize = 32.sp)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Type badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(typeColor)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = listing.type,
                            fontFamily = BangersFontFamily,
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = listing.title,
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 22.sp
                    )
                    if (listing.platform.isNotBlank()) {
                        Text(
                            text = listing.platform,
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (listing.price.isNotBlank() && listing.type == "FOR SALE") {
                            Text(
                                text = listing.price,
                                fontFamily = BangersFontFamily,
                                color = ScrapbookGreen,
                                fontSize = 18.sp
                            )
                        }
                        if (listing.condition.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ScrapbookPaper)
                                    .border(1.dp, ScrapbookBorder.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = listing.condition,
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = ScrapbookTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    // Seller row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(ScrapbookPaper)
                                .border(1.dp, ScrapbookBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (listing.sellerProfilePicUrl.isNotBlank()) {
                                AsyncImage(
                                    model = listing.sellerProfilePicUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = ScrapbookDark.copy(alpha = 0.4f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Text(
                            text = listing.sellerUsername,
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = ScrapbookTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ListingDetailScreen(
    listing: MarketplaceListing,
    currentUid: String,
    chatViewModel: ChatViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onViewSeller: (String) -> Unit
) {
    val myProfile by authViewModel.userProfile.collectAsState()
    var isStartingChat by remember { mutableStateOf(false) }
    val typeColor = when (listing.type) {
        "FOR SALE" -> ScrapbookGreen
        "FOR TRADE" -> ScrapbookBlue
        "WANTED" -> ScrapbookRed
        else -> ScrapbookDark
    }
    val dateStr = remember(listing.timestamp) {
        if (listing.timestamp > 0L)
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(listing.timestamp))
        else ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScrapbookCream)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                // Image hero
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    if (listing.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = listing.imageUrl,
                            contentDescription = listing.title,
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
                            Text("🎮", fontSize = 72.sp)
                        }
                    }
                    // Back button
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 40.dp, start = 8.dp)
                            .clip(CircleShape)
                            .background(ScrapbookYellow)
                            .border(2.dp, ScrapbookBorder, CircleShape)
                            .clickable { onBack() }
                            .padding(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ScrapbookDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // Type badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 40.dp, end = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(typeColor)
                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = listing.type,
                            fontFamily = BangersFontFamily,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title + price
                    Text(
                        text = listing.title,
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 28.sp,
                        lineHeight = 32.sp
                    )
                    if (listing.price.isNotBlank() && listing.type == "FOR SALE") {
                        Text(
                            text = listing.price,
                            fontFamily = BangersFontFamily,
                            color = ScrapbookGreen,
                            fontSize = 24.sp
                        )
                    }

                    // Details card
                    Box {
                        ScrapbookCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = ScrapbookCardWhite,
                            cornerRadius = 12.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (listing.platform.isNotBlank()) {
                                    GameDetailRow(label = "Platform", value = listing.platform)
                                }
                                if (listing.condition.isNotBlank()) {
                                    GameDetailRow(label = "Condition", value = listing.condition)
                                }
                                if (dateStr.isNotBlank()) {
                                    GameDetailRow(label = "Listed", value = dateStr)
                                }
                            }
                        }
                    }

                    // Description
                    if (listing.description.isNotBlank()) {
                        Text(
                            text = "DESCRIPTION",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 20.sp
                        )
                        Box {
                            ScrapbookCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = ScrapbookCardWhite,
                                cornerRadius = 12.dp
                            ) {
                                Text(
                                    text = listing.description,
                                    fontFamily = NunitoFontFamily,
                                    color = ScrapbookTextDark,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }

                    // Seller card
                    if (listing.sellerUid != currentUid) {
                        Text(
                            text = "SELLER",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 20.sp
                        )
                        Box {
                            ScrapbookCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = ScrapbookCardWhite,
                                cornerRadius = 12.dp
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onViewSeller(listing.sellerUid) },
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
                                            if (listing.sellerProfilePicUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = listing.sellerProfilePicUrl,
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
                                                text = listing.sellerUsername.uppercase(),
                                                fontFamily = BangersFontFamily,
                                                color = ScrapbookDark,
                                                fontSize = 18.sp
                                            )
                                            Text(
                                                text = "Tap to view profile →",
                                                fontFamily = NunitoFontFamily,
                                                color = ScrapbookTextMuted,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    // Message button
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(ScrapbookDark)
                                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp))
                                            .clickable(enabled = !isStartingChat) {
                                                isStartingChat = true
                                                val sellerData = UserProfileData(
                                                    uid = listing.sellerUid,
                                                    username = listing.sellerUsername,
                                                    profilePictureUrl = listing.sellerProfilePicUrl,
                                                    userHandle = "",
                                                    bio = "",
                                                    email = "",
                                                    setupComplete = true,
                                                    topGames = emptyList(),
                                                    topSoundtracks = emptyList()
                                                )
                                                val myProfileData = myProfile?.let {
                                                    UserProfileData(
                                                        uid = currentUid,
                                                        username = it.username,
                                                        profilePictureUrl = it.profilePictureUrl ?: "",
                                                        userHandle = it.userHandle,
                                                        bio = it.bio,
                                                        email = it.email,
                                                        setupComplete = it.setupComplete,
                                                        topGames = it.topGames,
                                                        topSoundtracks = it.topSoundtracks,
                                                        bannerUrl = it.bannerUrl
                                                    )
                                                }
                                                chatViewModel.getOrCreateDm(
                                                    otherUser = sellerData,
                                                    myProfile = myProfileData,
                                                    onResult = { isStartingChat = false }
                                                )
                                            }
                                            .padding(vertical = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isStartingChat) {
                                            CircularProgressIndicator(
                                                color = ScrapbookYellow,
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.Chat,
                                                    contentDescription = null,
                                                    tint = ScrapbookYellow,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "MESSAGE SELLER",
                                                    fontFamily = BangersFontFamily,
                                                    color = ScrapbookYellow,
                                                    fontSize = 18.sp
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
    }
}

@Composable
fun AddListingScreen(
    sellerUid: String,
    sellerUsername: String,
    sellerProfilePicUrl: String,
    onDismiss: () -> Unit,
    onSaved: (MarketplaceListing) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("FOR SALE") }
    var selectedCondition by remember { mutableStateOf("GOOD") }
    var selectedPlatform by remember { mutableStateOf("ANY") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> imageUri = uri }

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
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = ScrapbookDark)
                    }
                    Text(
                        text = "NEW LISTING",
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 26.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (title.isNotBlank() && !isSaving) ScrapbookDark
                                else ScrapbookDark.copy(alpha = 0.3f)
                            )
                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                            .clickable(enabled = title.isNotBlank() && !isSaving) {
                                isSaving = true
                                errorMsg = ""
                                val listingId = System.currentTimeMillis().toString()
                                val listingData = hashMapOf<String, Any>(
                                    "title" to title,
                                    "description" to description,
                                    "price" to price,
                                    "type" to selectedType,
                                    "condition" to selectedCondition,
                                    "platform" to selectedPlatform,
                                    "sellerUid" to sellerUid,
                                    "sellerUsername" to sellerUsername,
                                    "sellerProfilePicUrl" to sellerProfilePicUrl,
                                    "timestamp" to System.currentTimeMillis(),
                                    "imageUrl" to ""
                                )

                                fun saveListing(imageUrl: String) {
                                    listingData["imageUrl"] = imageUrl
                                    FirebaseFirestore.getInstance()
                                        .collection("marketplace")
                                        .document(listingId)
                                        .set(listingData)
                                        .addOnSuccessListener {
                                            onSaved(
                                                MarketplaceListing(
                                                    id = listingId,
                                                    title = title,
                                                    description = description,
                                                    price = price,
                                                    type = selectedType,
                                                    condition = selectedCondition,
                                                    platform = selectedPlatform,
                                                    sellerUid = sellerUid,
                                                    sellerUsername = sellerUsername,
                                                    sellerProfilePicUrl = sellerProfilePicUrl,
                                                    timestamp = System.currentTimeMillis(),
                                                    imageUrl = imageUrl
                                                )
                                            )
                                        }
                                        .addOnFailureListener {
                                            isSaving = false
                                            errorMsg = "Failed to save listing"
                                        }
                                }

                                if (imageUri != null) {
                                    val ref = FirebaseStorage.getInstance().reference
                                        .child("marketplace/$listingId.jpg")
                                    ref.putFile(imageUri!!)
                                        .addOnSuccessListener {
                                            ref.downloadUrl.addOnSuccessListener { url ->
                                                saveListing(url.toString())
                                            }
                                        }
                                        .addOnFailureListener { saveListing("") }
                                } else {
                                    saveListing("")
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = ScrapbookYellow,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("POST", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 16.sp)
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error
                if (errorMsg.isNotBlank()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ScrapbookRed.copy(alpha = 0.1f))
                                .border(1.dp, ScrapbookRed, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(errorMsg, fontFamily = NunitoFontFamily, color = ScrapbookRed, fontSize = 13.sp)
                        }
                    }
                }

                // Image picker
                item {
                    Text("PHOTO", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ScrapbookPaper)
                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(12.dp))
                            .clickable { imageLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUri != null) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = null,
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
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = ScrapbookDark.copy(alpha = 0.3f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "TAP TO ADD PHOTO",
                                    fontFamily = BangersFontFamily,
                                    color = ScrapbookDark.copy(alpha = 0.4f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Listing type
                item {
                    Text("LISTING TYPE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("FOR SALE", "FOR TRADE", "WANTED").forEach { type ->
                            val isSelected = selectedType == type
                            val color = when (type) {
                                "FOR SALE" -> ScrapbookGreen
                                "FOR TRADE" -> ScrapbookBlue
                                else -> ScrapbookRed
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) color else ScrapbookPaper)
                                    .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                                    .clickable { selectedType = type }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = type,
                                    fontFamily = BangersFontFamily,
                                    color = if (isSelected) Color.White else ScrapbookDark,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Title
                item {
                    ScrapbookInputField(
                        value = title,
                        onValueChange = { title = it },
                        label = "GAME TITLE *"
                    )
                }

                // Price (only for FOR SALE)
                if (selectedType == "FOR SALE") {
                    item {
                        ScrapbookInputField(
                            value = price,
                            onValueChange = { price = it },
                            label = "PRICE (e.g. \$15)"
                        )
                    }
                }

                // Platform
                item {
                    Text("PLATFORM", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(gamePlatformsList) { platform ->
                            val isSelected = selectedPlatform == platform
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) ScrapbookDark else ScrapbookCardWhite)
                                    .border(2.dp, ScrapbookBorder, RoundedCornerShape(20.dp))
                                    .clickable { selectedPlatform = platform }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = platform,
                                    fontFamily = BangersFontFamily,
                                    color = if (isSelected) ScrapbookYellow else ScrapbookDark,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Condition
                item {
                    Text("CONDITION", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gameConditions.forEach { condition ->
                            val isSelected = selectedCondition == condition
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ScrapbookYellow else ScrapbookCardWhite)
                                    .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                                    .clickable { selectedCondition = condition }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = condition,
                                    fontFamily = BangersFontFamily,
                                    color = ScrapbookDark,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Description
                item {
                    ScrapbookInputField(
                        value = description,
                        onValueChange = { description = it },
                        label = "DESCRIPTION"
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}
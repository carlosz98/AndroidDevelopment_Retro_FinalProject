package com.example.hubretro

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// ─── Brand colors ─────────────────────────────────────────────────────────────
val PayPalBlue    = CAcBlue
val PayPalGold    = CAcYellow
val EbayRed       = CAcRed
val AmazonOrange  = Color(0xFFFF9900)
val MercariRed    = CAcRed
val EtsyOrange    = CAcRed
val FbBlue        = CAcBlue
val MarketOrange  = CAcYellow

// ─── Data classes ─────────────────────────────────────────────────────────────

data class MarketplaceListing(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val price: String = "",
    val condition: String = "",
    val type: String = "FOR SALE",
    val imageUrl: String = "",
    val sellerUid: String = "",
    val sellerUsername: String = "",
    val sellerProfilePicUrl: String = "",
    val timestamp: Long = 0L,
    val platform: String = "",
    val paypalUsername: String = ""
)

data class CommunityFind(
    val id: String = "",
    val uid: String = "",
    val username: String = "",
    val profilePicUrl: String = "",
    val itemName: String = "",
    val pricePaid: String = "",
    val source: String = "",
    val imageUrl: String = "",
    val note: String = "",
    val timestamp: Long = 0L
)

data class RetroMarketResult(
    val name: String,
    val logoResId: Int,
    val color: Color,
    val buildUrl: (String) -> String
)

data class WatchedSearch(
    val id: String = "",
    val query: String = "",
    val lastCheckedTimestamp: Long = 0L,
    val lastKnownCount: Int = 0,
    val lastKnownLowestPrice: String = "",
    val createdAt: Long = 0L
)

// ─── Constants ────────────────────────────────────────────────────────────────

val listingTypes       = listOf("ALL", "FOR SALE", "FOR TRADE", "WANTED")
val gameConditions     = listOf("MINT", "EXCELLENT", "GOOD", "FAIR", "POOR")
val gamePlatformsList  = listOf("ANY","NES","SNES","N64","GAMEBOY","PS1","PS2","PS3","SEGA","ARCADE","PC","OTHER")

val retroMarkets = listOf(
    RetroMarketResult("eBay",     R.drawable.logo_ebay,     EbayRed)     { q -> "https://www.ebay.com/sch/i.html?_nkw=${Uri.encode(q)}&_sop=12" },
    RetroMarketResult("Amazon",   R.drawable.logo_amazon,   AmazonOrange) { q -> "https://www.amazon.com/s?k=${Uri.encode(q)}&s=price-asc-rank" },
    RetroMarketResult("Facebook", R.drawable.logo_facebook, FbBlue)      { q -> "https://www.facebook.com/marketplace/search/?query=${Uri.encode(q)}" },
    RetroMarketResult("Mercari",  R.drawable.logo_mercari,  MercariRed)  { q -> "https://www.mercari.com/search/?keyword=${Uri.encode(q)}&status=on_sale" },
    RetroMarketResult("Etsy",     R.drawable.logo_etsy,     EtsyOrange)  { q -> "https://www.etsy.com/search?q=${Uri.encode(q)}&min_price=1" }
)

val quickCategories = listOf(
    "🎮 NES Games", "🟣 SNES Games", "💙 PS1 Games", "🌐 N64 Games",
    "💿 SEGA Genesis", "📱 Game Boy", "🟣 GameCube", "🔵 PS2 Games",
    "🕹️ Arcade Sticks", "📺 CRT Monitor", "🃏 Gaming Cards",
    "💎 Sealed Games", "📦 Complete In Box", "🏆 Graded Games"
)

fun getRarity(query: String): Triple<String, String, Color> {
    val q = query.lowercase()
    return when {
        q.contains("sealed") || q.contains("graded") || q.contains("wata") || q.contains("vga") ->
            Triple("💎 ULTRA RARE", "Sealed/Graded copies are extremely scarce", CAcPurple)
        q.contains("cib") || q.contains("complete in box") ->
            Triple("🔴 RARE", "Complete-in-box items are hard to find in good condition", EbayRed)
        q.contains("mint") || q.contains("limited") || q.contains("prototype") ->
            Triple("🟠 UNCOMMON", "Mint or limited editions fetch premium prices", AmazonOrange)
        q.contains("loose") || q.contains("cartridge only") ->
            Triple("🟢 COMMON", "Loose cartridges are widely available", CGreen)
        else ->
            Triple("🟡 STANDARD", "Availability varies by condition and region", CGreenDeep)
    }
}

fun conditionColor(condition: String): Color = when (condition.lowercase()) {
    "new"                       -> CGreen
    "like new", "very good"     -> CGreenMint
    "good"                      -> CGreenDeep
    "acceptable","fair","poor"  -> CAcRed
    else                        -> ScrapbookTextMuted
}

// ─── Main Screen ──────────────────────────────────────────────────────────────

@Composable
fun MarketplaceScreen(
    authViewModel: AuthViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val currentUser      by authViewModel.currentUser.collectAsState()
    val firebaseProfile  by authViewModel.userProfile.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("🔍 FIND IT", "📌 SAVED", "🛒 COMMUNITY")

    val neonT = rememberInfiniteTransition(label = "mktNeon")
    val neonAlpha by rememberGlowRange(0.4f, 1f)

    var listings       by remember { mutableStateOf<List<MarketplaceListing>>(emptyList()) }
    var isLoading      by remember { mutableStateOf(true) }
    var selectedType   by remember { mutableStateOf("ALL") }
    var showAddListing by remember { mutableStateOf(false) }
    var selectedListing by remember { mutableStateOf<MarketplaceListing?>(null) }
    var selectedSeller  by remember { mutableStateOf<UserProfileData?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val docs = FirebaseFirestore.getInstance()
                .collection("marketplace")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50).get().await()
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
                    platform = data["platform"] as? String ?: "",
                    paypalUsername = data["paypalUsername"] as? String ?: ""
                )
            }.filter { it.title.isNotBlank() }
        } catch (e: Exception) { } finally { isLoading = false }
    }

    val filteredListings = remember(listings, selectedType) {
        if (selectedType == "ALL") listings else listings.filter { it.type == selectedType }
    }

    // Sub-screen routing
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
    if (selectedListing != null) {
        ListingDetailScreen(
            listing = selectedListing!!,
            currentUid = currentUser?.uid ?: "",
            chatViewModel = chatViewModel,
            authViewModel = authViewModel,
            onBack = { selectedListing = null },
            onViewSeller = { uid ->
                FirebaseFirestore.getInstance().collection("users").document(uid).get()
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
                            topGames = emptyList(), topSoundtracks = emptyList()
                        )
                    }
            }
        )
        return
    }
    if (showAddListing) {
        AddListingScreen(
            sellerUid = currentUser?.uid ?: "",
            sellerUsername = firebaseProfile?.username ?: "",
            sellerProfilePicUrl = firebaseProfile?.profilePictureUrl ?: "",
            onDismiss = { showAddListing = false },
            onSaved = { listing -> listings = listOf(listing) + listings; showAddListing = false }
        )
        return
    }

    Box(modifier = modifier.fillMaxSize().background(ComicGlassBg)) {
        HalftoneBackground(modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize()) {

            // ─── Toolbar ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.92f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(MarketOrange.copy(alpha = 0.15f))
                            .border(1.dp, MarketOrange.copy(alpha = neonAlpha * 0.8f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            val dotPulse by neonT.animateFloat(
                                initialValue = 0.4f, targetValue = 1f,
                                animationSpec = infiniteRepeatable(tween(600, easing = EaseInOut), RepeatMode.Reverse),
                                label = "openDot"
                            )
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(MarketOrange.copy(alpha = dotPulse)))
                            Text("OPEN", fontFamily = BangersFontFamily, color = MarketOrange, fontSize = 13.sp, letterSpacing = 1.sp)
                        }
                    }
                    if (selectedTab == 2 && currentUser != null && firebaseProfile?.setupComplete == true) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .border(1.dp, CGreen.copy(alpha = 0.5f), CircleShape)
                                .clickable { showAddListing = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add listing", tint = CGreen, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // ─── Tab strip (ink underline) ───────────────────────────────────
            InkTabRow(
                tabs = tabs,
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )

            when (selectedTab) {
                0 -> FindItTab(neonAlpha = neonAlpha, currentUser = currentUser)
                1 -> SavedSearchesTab(neonAlpha = neonAlpha, currentUser = currentUser)
                2 -> CommunityTab(
                    listings = filteredListings,
                    isLoading = isLoading,
                    selectedType = selectedType,
                    onTypeSelected = { selectedType = it },
                    onListingClick = { selectedListing = it },
                    neonAlpha = neonAlpha
                )
            }
        }
    }
}

// ─── FIND IT TAB ──────────────────────────────────────────────────────────────

@Composable
fun FindItTab(neonAlpha: Float, currentUser: Any?) {
    val context      = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope        = rememberCoroutineScope()

    var searchQuery   by remember { mutableStateOf("") }
    var hasSearched   by remember { mutableStateOf(false) }
    var recentSearches by remember { mutableStateOf<List<String>>(emptyList()) }
    var isQuerySaved   by remember { mutableStateOf(false) }
    var resultsCache   by remember { mutableStateOf<Map<String, Pair<Long, List<EbayListing>>>>(emptyMap()) }

    // eBay live results
    var ebayListings    by remember { mutableStateOf<List<EbayListing>>(emptyList()) }
    var isLoadingEbay   by remember { mutableStateOf(false) }
    var ebayError       by remember { mutableStateOf("") }
    var selectedEbayListing by remember { mutableStateOf<EbayListing?>(null) }
    var ebaySort        by remember { mutableStateOf("newlyListed") }

    // Community finds
    var communityFinds       by remember { mutableStateOf<List<CommunityFind>>(emptyList()) }
    var showCommunityFindDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val docs = FirebaseFirestore.getInstance()
                .collection("community_finds")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20).get().await()
            communityFinds = docs.documents.mapNotNull { doc ->
                val d = doc.data ?: return@mapNotNull null
                CommunityFind(
                    id = doc.id, uid = d["uid"] as? String ?: "",
                    username = d["username"] as? String ?: "",
                    profilePicUrl = d["profilePicUrl"] as? String ?: "",
                    itemName = d["itemName"] as? String ?: "",
                    pricePaid = d["pricePaid"] as? String ?: "",
                    source = d["source"] as? String ?: "",
                    imageUrl = d["imageUrl"] as? String ?: "",
                    note = d["note"] as? String ?: "",
                    timestamp = d["timestamp"] as? Long ?: 0L
                )
            }.filter { it.itemName.isNotBlank() }
        } catch (e: Exception) { }
    }

    fun doSearch(query: String) {
        if (query.isBlank()) return
        hasSearched = true
        if (!recentSearches.contains(query)) recentSearches = listOf(query) + recentSearches.take(9)
        focusManager.clearFocus()

        // Check watchlist status
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("watchlist").document(query.lowercase().trim())
                .get()
                .addOnSuccessListener { doc -> isQuerySaved = doc.exists() }
        } else {
            isQuerySaved = false
        }

        // Cache check (5 min freshness)
        val cached = resultsCache[query.lowercase()]
        val cacheAgeMs = cached?.let { System.currentTimeMillis() - it.first } ?: Long.MAX_VALUE
        if (cached != null && cacheAgeMs < 5 * 60 * 1000L) {
            ebayListings = cached.second
            ebayError = if (ebayListings.isEmpty()) "No eBay listings found for \"$query\"" else ""
            isLoadingEbay = false
            return
        }

        scope.launch {
            isLoadingEbay = true
            ebayError = ""
            ebayListings = emptyList()
            try {
                val results = EbayRepository.searchListings(query, sortBy = ebaySort)
                ebayListings = results
                resultsCache = resultsCache + (query.lowercase() to (System.currentTimeMillis() to results))
                if (results.isEmpty()) ebayError = "No eBay listings found for \"$query\""
            } catch (e: Exception) {
                ebayError = "Couldn't load eBay listings. Check your connection."
            }
            isLoadingEbay = false
        }
    }

    // Deep link from other pages (e.g. Game Database → "Find it for sale")
    val pendingMarketQuery = MarketplaceSearchBus.pendingQuery
    LaunchedEffect(pendingMarketQuery) {
        val q = pendingMarketQuery ?: return@LaunchedEffect
        MarketplaceSearchBus.pendingQuery = null
        searchQuery = q
        doSearch(q)
    }

    fun toggleWatchlist(query: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val docId = query.lowercase().trim()
        val lowestPrice = ebayListings.minByOrNull { it.price.toDoubleOrNull() ?: Double.MAX_VALUE }?.price ?: ""
        if (isQuerySaved) {
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("watchlist").document(docId).delete()
            isQuerySaved = false
        } else {
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("watchlist").document(docId)
                .set(hashMapOf(
                    "query" to query,
                    "lastCheckedTimestamp" to System.currentTimeMillis(),
                    "lastKnownCount" to ebayListings.size,
                    "lastKnownLowestPrice" to lowestPrice,
                    "createdAt" to System.currentTimeMillis()
                ))
            isQuerySaved = true
        }
    }

    // Detail screen overlay
    if (selectedEbayListing != null) {
        EbayListingDetailScreen(
            listing = selectedEbayListing!!,
            onBack = { selectedEbayListing = null },
            onOpenEbay = {
                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(selectedEbayListing!!.itemUrl))) }
                catch (e: Exception) { }
            }
        )
        return
    }

    if (showCommunityFindDialog && currentUser != null) {
        CommunityFindPostDialog(
            onDismiss = { showCommunityFindDialog = false },
            onPosted   = { find -> communityFinds = listOf(find) + communityFinds; showCommunityFindDialog = false }
        )
    }

    val rarity = remember(searchQuery) { if (searchQuery.length >= 3) getRarity(searchQuery) else null }

    // Search debounce — auto-search as user types
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 3) {
            delay(600)
            doSearch(searchQuery)
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {

        // ─── Page Hero ────────────────────────────────────────────────────────
        item {
            RetroHubPageHero(config = marketplaceHeroConfig, onCtaClick = { })
        }
        item { RetroHubPageTicker(config = marketplaceHeroConfig) }

        // ─── CRT Search Terminal ──────────────────────────────────────────────
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Box(
                    modifier = Modifier.matchParentSize()
                        .offset(x = 4.dp, y = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ScrapbookDark.copy(alpha = 0.12f))
                )
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ComicGlassBg)
                        .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(16.dp))
                ) {
                val scanT = rememberInfiniteTransition(label = "crtScan")
                val scanY by scanT.animateFloat(
                    initialValue = -300f, targetValue = 300f,
                    animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
                    label = "crtScanY"
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).offset(y = scanY.dp).background(MarketOrange.copy(alpha = 0.06f)))

                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(CAcRed))
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(CAcYellow))
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(CGreen))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RETROMARKET_SEARCH.exe", fontFamily = BangersFontFamily, color = MarketOrange.copy(alpha = 0.5f), fontSize = 11.sp, letterSpacing = 1.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ComicGlassBg)
                            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(">_", fontFamily = BangersFontFamily, color = MarketOrange, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it; if (it.isBlank()) { hasSearched = false; ebayListings = emptyList(); isQuerySaved = false } },
                            placeholder = { Text("Search any game, console, accessory...", fontFamily = NunitoFontFamily, color = MarketOrange.copy(alpha = 0.35f), fontSize = 13.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { doSearch(searchQuery) }),
                            textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = MarketOrange),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                cursorColor = MarketOrange
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = ""; hasSearched = false; ebayListings = emptyList(); isQuerySaved = false }) {
                                        Icon(Icons.Filled.Close, contentDescription = null, tint = MarketOrange.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    var searchPressed by remember { mutableStateOf(false) }
                    val searchScale by animateFloatAsState(targetValue = if (searchPressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "searchBtnScale")
                    Box(
                        modifier = Modifier.fillMaxWidth().scale(searchScale)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (searchQuery.isNotBlank()) Brush.horizontalGradient(colors = listOf(MarketOrange, Color(0xFFFF8C42))) else Brush.horizontalGradient(colors = listOf(MarketOrange.copy(alpha = 0.3f), MarketOrange.copy(alpha = 0.3f))))
                            .clickable(enabled = searchQuery.isNotBlank()) { searchPressed = true; doSearch(searchQuery) }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text("HUNT FOR DEALS →", fontFamily = BangersFontFamily, color = Color.White, fontSize = 18.sp, letterSpacing = 1.sp)
                        }
                    }
                    LaunchedEffect(searchPressed) { if (searchPressed) { delay(150); searchPressed = false } }
                }
                }
            }
        }

        // ─── Recent searches ──────────────────────────────────────────────────
        if (recentSearches.isNotEmpty() && !hasSearched) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.width(3.dp).height(16.dp).clip(RoundedCornerShape(2.dp)).background(MarketOrange.copy(alpha = neonAlpha)))
                        Text("RECENT", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp, letterSpacing = 1.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recentSearches) { recent ->
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                    .background(ScrapbookDark)
                                    .border(2.5.dp, ScrapbookDark, RoundedCornerShape(20.dp))
                                    .clickable { searchQuery = recent; doSearch(recent) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🕐", fontSize = 11.sp)
                                    Text(recent, fontFamily = BangersFontFamily, color = MarketOrange, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // ─── Quick Categories ─────────────────────────────────────────────────
        if (!hasSearched) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.width(3.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(MarketOrange.copy(alpha = neonAlpha)))
                        Text("QUICK SEARCH", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, letterSpacing = 1.sp)
                    }
                    Text("Tap a category to start hunting", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    val rows = quickCategories.chunked(2)
                    for (rowItems in rows) {
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (cat in rowItems) {
                                var catPressed by remember { mutableStateOf(false) }
                                val catScale by animateFloatAsState(targetValue = if (catPressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "cat_$cat")
                                Box(
                                    modifier = Modifier.weight(1f).scale(catScale)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.92f))
                                        .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(12.dp))
                                        .clickable {
                                            catPressed = true
                                            val cleanCat = cat.replace(Regex("^[^a-zA-Z]+"), "").trim()
                                            searchQuery = cleanCat
                                            doSearch(cleanCat)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Text(cat, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                LaunchedEffect(catPressed) { if (catPressed) { delay(150); catPressed = false } }
                            }
                            if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = ScrapbookBorder.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // ─── RESULTS ─────────────────────────────────────────────────────────
        if (hasSearched && searchQuery.isNotBlank()) {

            rarity?.let { (label, desc, color) ->
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(color.copy(alpha = 0.1f))
                            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(label.split(" ").first(), fontSize = 24.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, fontFamily = BangersFontFamily, color = color, fontSize = 16.sp)
                                Text(desc, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, lineHeight = 15.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // ─── eBay LIVE Results header ───────────────────────────────────────
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Image(painter = painterResource(id = R.drawable.logo_ebay), contentDescription = "eBay", modifier = Modifier.height(22.dp), contentScale = ContentScale.Fit)
                        Text("LIVE LISTINGS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                        if (!isLoadingEbay && ebayListings.isNotEmpty()) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookDark).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("${ebayListings.size}", fontFamily = BangersFontFamily, color = EbayRed, fontSize = 13.sp)
                            }
                        }
                        // Watchlist bookmark
                        if (currentUser != null && !isLoadingEbay) {
                            var bookmarkPressed by remember { mutableStateOf(false) }
                            val bookmarkScale by animateFloatAsState(targetValue = if (bookmarkPressed) 1.3f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy), label = "bookmarkScale")
                            Box(
                                modifier = Modifier.scale(bookmarkScale).size(34.dp).clip(CircleShape)
                                    .background(if (isQuerySaved) CGreen else Color.White.copy(alpha = 0.46f))
                                    .border(1.5.dp, if (isQuerySaved) CGreenDeep else ScrapbookBorder, CircleShape)
                                    .clickable { bookmarkPressed = true; toggleWatchlist(searchQuery) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isQuerySaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                    contentDescription = "Save search",
                                    tint = if (isQuerySaved) Color.White else ScrapbookDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            LaunchedEffect(bookmarkPressed) { if (bookmarkPressed) { delay(200); bookmarkPressed = false } }
                        }
                    }

                    // Average price badge
                    if (!isLoadingEbay && ebayListings.isNotEmpty()) {
                        val avgPrice = ebayListings.mapNotNull { it.price.toDoubleOrNull() }.let { if (it.isNotEmpty()) it.average() else null }
                        if (avgPrice != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(CGreen.copy(alpha = 0.1f))
                                    .border(2.5.dp, ScrapbookDark, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text("📊 AVG PRICE: $${String.format("%.2f", avgPrice)}", fontFamily = BangersFontFamily, color = CGreen, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    // Sort chips
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for ((sort, label) in listOf("newlyListed" to "NEW", "price" to "LOWEST $", "bestMatch" to "BEST MATCH")) {
                            val isSelected = ebaySort == sort
                            var sortPressed by remember { mutableStateOf(false) }
                            val sortScale by animateFloatAsState(targetValue = if (sortPressed) 0.9f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "sort_$sort")
                            Box(
                                modifier = Modifier.scale(sortScale)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) EbayRed else Color.White.copy(alpha = 0.46f))
                                    .border(1.dp, if (isSelected) EbayRed else ScrapbookBorder, RoundedCornerShape(20.dp))
                                    .clickable { sortPressed = true; ebaySort = sort; doSearch(searchQuery) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(label, fontFamily = BangersFontFamily, color = if (isSelected) Color.White else ScrapbookDark.copy(alpha = 0.6f), fontSize = 11.sp)
                            }
                            LaunchedEffect(sortPressed) { if (sortPressed) { delay(150); sortPressed = false } }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            when {
                isLoadingEbay -> {
                    items(3) { ShimmerEbayCard() }
                }
                ebayError.isNotBlank() -> {
                    item {
                        if (ebayError.startsWith("Couldn't")) GameOverState(message = ebayError, onRetry = null)
                        else Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CAcRed.copy(alpha = 0.08f))
                                .border(1.dp, CAcRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text("📡", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(ebayError, fontFamily = NunitoFontFamily, color = CAcRed, fontSize = 13.sp, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Try the other markets below!", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                else -> {
                    val lowestPriceItemId = ebayListings.minByOrNull { it.price.toDoubleOrNull() ?: Double.MAX_VALUE }?.itemId
                    itemsIndexed(ebayListings, key = { _, l -> l.itemId }) { index, listing ->
                        EbayListingCard(
                            listing = listing,
                            neonAlpha = neonAlpha,
                            index = index,
                            isBestDeal = listing.itemId == lowestPriceItemId && ebayListings.size > 1,
                            onTap = { selectedEbayListing = listing }
                        )
                    }
                }
            }

            // ─── External Marketplaces ────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(3.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(MarketOrange.copy(alpha = neonAlpha)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MORE MARKETS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Tap to search on each platform", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(retroMarkets.filter { it.name != "eBay" }) { market ->
                MarketResultCard(market = market, query = searchQuery, neonAlpha = neonAlpha, context = context)
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                BuyingTipsCard(query = searchQuery, neonAlpha = neonAlpha)
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                PriceIntelligenceCard(query = searchQuery, neonAlpha = neonAlpha, context = context)
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = ScrapbookBorder.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // ─── Community Finds ──────────────────────────────────────────────────
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(3.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(alpha = neonAlpha)))
                Spacer(modifier = Modifier.width(8.dp))
                Text("🏆 COMMUNITY FINDS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                if (currentUser != null) {
                    var addPressed by remember { mutableStateOf(false) }
                    val addScale by animateFloatAsState(targetValue = if (addPressed) 0.9f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "addFindScale")
                    Box(
                        modifier = Modifier.scale(addScale).clip(RoundedCornerShape(8.dp))
                            .background(ScrapbookDark)
                            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(8.dp))
                            .clickable { addPressed = true; showCommunityFindDialog = true }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("+ POST FIND", fontFamily = BangersFontFamily, color = CGreen, fontSize = 12.sp)
                    }
                    LaunchedEffect(addPressed) { if (addPressed) { delay(150); addPressed = false } }
                }
            }
            Text("Real deals found by the RetroHub community", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (communityFinds.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏆", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("NO FINDS YET", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp)
                        Text(if (currentUser != null) "Be the first to post a community find!" else "Sign in to post your best deals", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            itemsIndexed(communityFinds, key = { _, f -> f.id }) { index, find ->
                CommunityFindCard(find = find, neonAlpha = neonAlpha, index = index)
            }
        }
    }
}

// ─── Shimmer eBay Card ────────────────────────────────────────────────────────

@Composable
fun ShimmerEbayCard() {
    val shimmerT = rememberInfiniteTransition(label = "ebayShimmer")
    val shimmerX by shimmerT.animateFloat(
        initialValue = -600f, targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "ebayShimmerX"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(Color.White.copy(alpha = 0.46f), Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.46f)),
        start = androidx.compose.ui.geometry.Offset(shimmerX - 200f, 0f),
        end = androidx.compose.ui.geometry.Offset(shimmerX + 200f, 0f)
    )
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ComicGlassBg)
            .border(1.dp, ScrapbookBorder.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.size(90.dp).clip(RoundedCornerShape(10.dp)).background(shimmerBrush))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.fillMaxWidth(0.85f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
                Box(modifier = Modifier.fillMaxWidth(0.4f).height(20.dp).clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
                Box(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
            }
        }
    }
}

// ─── eBay Listing Card ────────────────────────────────────────────────────────

@Composable
fun EbayListingCard(listing: EbayListing, neonAlpha: Float, index: Int, isBestDeal: Boolean = false, onTap: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val glowAlpha by rememberGlowPhase(0.4f)
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "ebayPress_$index")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "ebayShadow_$index")

    val condColor = conditionColor(listing.condition)

    val kenBurnsT = rememberInfiniteTransition(label = "ebayKenBurns_$index")
    val kenBurns by kenBurnsT.animateFloat(initialValue = 1f, targetValue = 1.06f, animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse), label = "ebayKenBurnsScale_$index")

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)) {
        Box(modifier = Modifier.matchParentSize().offset(x = 6.dp, y = 6.dp).clip(RoundedCornerShape(14.dp)).background(CGreen.copy(alpha = glowAlpha * 0.28f)))
        Box(modifier = Modifier.matchParentSize().offset(x = shadowOff.dp, y = shadowOff.dp).clip(RoundedCornerShape(14.dp)).background(CGreen))
        if (isBestDeal) {
            Box(modifier = Modifier.matchParentSize().padding(3.dp).blur(14.dp).background(Color(0xFFFFD700).copy(alpha = neonAlpha * 0.3f), RoundedCornerShape(14.dp)))
        } else {
            Box(modifier = Modifier.matchParentSize().padding(3.dp).blur(10.dp).background(EbayRed.copy(alpha = neonAlpha * 0.08f), RoundedCornerShape(14.dp)))
        }
        Box(
            modifier = Modifier.fillMaxWidth()
                .offset(x = pressAnim.dp, y = pressAnim.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ComicGlassBg)
                .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(14.dp))
                .clickable { pressed = true; onTap() }
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Condition color strip
                Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(condColor))

                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {

                    // Cover image
                    Box(
                        modifier = Modifier.size(90.dp).clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.92f))
                            .border(1.dp, ScrapbookBorder, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (listing.imageUrl.isNotBlank()) {
                            AsyncImage(model = listing.imageUrl, contentDescription = listing.title, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(listing.imageUrl).fillMaxSize().scale(kenBurns))
                        } else { Text("🎮", fontSize = 32.sp) }
                        Box(
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                .background(EbayRed.copy(alpha = 0.88f)).padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(painter = painterResource(id = R.drawable.logo_ebay), contentDescription = null, modifier = Modifier.height(10.dp), contentScale = ContentScale.Fit)
                        }
                        if (isBestDeal) {
                            Box(modifier = Modifier.align(Alignment.TopStart).padding(4.dp)) {
                                Text("👑", fontSize = 18.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        if (isBestDeal) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFFFD700)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("BEST DEAL", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 9.sp, letterSpacing = 0.5.sp)
                            }
                        }
                        Text(listing.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp)

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "${if (listing.currency == "USD") "$" else listing.currency}${listing.price}",
                                fontFamily = BangersFontFamily, color = EbayRed, fontSize = 20.sp
                            )
                            if (listing.shippingCost.isNotBlank() && listing.shippingCost != "?") {
                                Text(
                                    if (listing.shippingCost == "0.0" || listing.shippingCost == "0") "FREE SHIPPING"
                                    else "+$${listing.shippingCost} ship",
                                    fontFamily = NunitoFontFamily,
                                    color = if (listing.shippingCost == "0.0" || listing.shippingCost == "0") CGreen else ScrapbookTextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                    .background(condColor.copy(alpha = 0.12f))
                                    .border(1.dp, condColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(listing.condition, fontFamily = BangersFontFamily, color = condColor, fontSize = 10.sp)
                            }
                            if (listing.location.isNotBlank()) {
                                Text("📍 ${listing.location}", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        if (listing.seller.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = ScrapbookTextMuted, modifier = Modifier.size(12.dp))
                                Text(listing.seller, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                if (listing.sellerFeedback > 0) {
                                    Text("(${listing.sellerFeedback})", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(EbayRed)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text("VIEW LISTING →", fontFamily = BangersFontFamily, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── eBay Listing Detail Screen ───────────────────────────────────────────────

@Composable
fun EbayListingDetailScreen(listing: EbayListing, onBack: () -> Unit, onOpenEbay: () -> Unit) {
    val neonAlpha by rememberGlowRange(0.4f, 1f)

    val condColor = conditionColor(listing.condition)
    var selectedImageIndex by remember { mutableStateOf(0) }
    val allImages = remember(listing) { listOf(listing.imageUrl).plus(listing.additionalImages).filter { it.isNotBlank() } }

    Box(modifier = Modifier.fillMaxSize().background(ComicGlassBg)) {
        HalftoneBackground(modifier = Modifier.fillMaxSize())
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(280.dp).background(ScrapbookDark)) {
                    if (allImages.isNotEmpty()) {
                        AsyncImage(model = allImages[selectedImageIndex], contentDescription = listing.title, contentScale = ContentScale.Fit, modifier = Modifier.halftoneReveal(allImages[selectedImageIndex]).fillMaxSize())
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("🎮", fontSize = 72.sp) }
                    }
                    Box(
                        modifier = Modifier.align(Alignment.TopStart).padding(top = 40.dp, start = 12.dp)
                            .clip(CircleShape).background(CGreen).border(2.dp, ScrapbookBorder, CircleShape)
                            .clickable { onBack() }.padding(8.dp)
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 12.dp)
                            .clip(RoundedCornerShape(8.dp)).background(EbayRed)
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Image(painter = painterResource(id = R.drawable.logo_ebay), contentDescription = null, modifier = Modifier.height(16.dp), contentScale = ContentScale.Fit)
                    }
                }
                if (allImages.size > 1) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().background(ScrapbookDark).padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(allImages) { i, imgUrl ->
                            Box(
                                modifier = Modifier.size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(2.dp, if (i == selectedImageIndex) EbayRed else Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .clickable { selectedImageIndex = i }
                            ) {
                                AsyncImage(model = imgUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(imgUrl).fillMaxSize())
                            }
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(listing.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, lineHeight = 28.sp)

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.swingIn(listing.title)
                                .clip(RoundedCornerShape(8.dp)).background(CAcYellowL)
                                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(8.dp))
                                .padding(start = 8.dp, end = 12.dp, top = 3.dp, bottom = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // string hole
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color.White).border(1.5.dp, ScrapbookDark, CircleShape))
                                Text("${if (listing.currency == "USD") "$" else listing.currency}${listing.price}", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 30.sp)
                            }
                        }
                        Column {
                            if (listing.shippingCost == "0.0" || listing.shippingCost == "0") {
                                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(CGreen.copy(alpha = 0.15f)).border(2.5.dp, ScrapbookDark, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                    Text("FREE SHIPPING", fontFamily = BangersFontFamily, color = CGreen, fontSize = 12.sp)
                                }
                            } else if (listing.shippingCost.isNotBlank() && listing.shippingCost != "?") {
                                Text("+$${listing.shippingCost} shipping", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ComicGlassBg).border(1.dp, ScrapbookBorder.copy(alpha = 0.3f), RoundedCornerShape(14.dp)).padding(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Condition", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(condColor.copy(alpha = 0.12f)).border(1.dp, condColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                    Text(listing.condition, fontFamily = BangersFontFamily, color = condColor, fontSize = 13.sp)
                                }
                            }
                            if (listing.location.isNotBlank()) {
                                HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.2f))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Location", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("📍 ${listing.location}", fontFamily = NunitoFontFamily, color = ScrapbookDark, fontSize = 13.sp)
                                }
                            }
                            if (listing.listingType.isNotBlank()) {
                                HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.2f))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Type", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(listing.listingType, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                                }
                            }
                            if (listing.seller.isNotBlank()) {
                                HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.2f))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Seller", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(listing.seller, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                                        if (listing.sellerFeedback > 0) {
                                            Text("⭐ ${listing.sellerFeedback} feedback", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (listing.shortDescription.isNotBlank()) {
                        Text("DESCRIPTION", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp)
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ComicGlassBg).border(1.dp, ScrapbookBorder.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(14.dp)) {
                            Text(listing.shortDescription, fontFamily = NunitoFontFamily, color = ScrapbookDark, fontSize = 14.sp, lineHeight = 21.sp)
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(EbayRed.copy(alpha = 0.06f))
                            .border(1.dp, EbayRed.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("🔒", fontSize = 18.sp)
                            Text(
                                "This listing is on eBay. RetroHub shows you the details — tapping the button below takes you to the actual eBay listing to purchase safely.",
                                fontFamily = NunitoFontFamily, color = EbayRed.copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(colors = listOf(Color.Transparent, ComicGlassBg)))
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            var buyPressed by remember { mutableStateOf(false) }
            val buyPressAnim by animateFloatAsState(if (buyPressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "buyPressAnim")
            val buyShadowOff by animateFloatAsState(if (buyPressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "buyShadowOff")
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.matchParentSize().offset(x = buyShadowOff.dp, y = buyShadowOff.dp).clip(RoundedCornerShape(14.dp)).background(ScrapbookDark))
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .offset(x = buyPressAnim.dp, y = buyPressAnim.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient(colors = listOf(EbayRed, CAcRed)))
                        .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(14.dp))
                        .clickable { buyPressed = true; onOpenEbay() }
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Image(painter = painterResource(id = R.drawable.logo_ebay), contentDescription = null, modifier = Modifier.height(20.dp), contentScale = ContentScale.Fit)
                        Text("VIEW ON EBAY →", fontFamily = BangersFontFamily, color = Color.White, fontSize = 20.sp, letterSpacing = 1.sp)
                    }
                }
            }
            LaunchedEffect(buyPressed) { if (buyPressed) { delay(150); buyPressed = false } }
        }
    }
}

// ─── SAVED SEARCHES TAB ───────────────────────────────────────────────────────

@Composable
fun SavedSearchesTab(neonAlpha: Float, currentUser: Any?) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var watchedSearches by remember { mutableStateOf<List<WatchedSearch>>(emptyList()) }
    var isLoading        by remember { mutableStateOf(true) }
    var selectedEbayListing by remember { mutableStateOf<EbayListing?>(null) }
    var liveResultsMap by remember { mutableStateOf<Map<String, List<EbayListing>>>(emptyMap()) }
    var checkingId by remember { mutableStateOf<String?>(null) }

    val uid = FirebaseAuth.getInstance().currentUser?.uid

    fun loadWatchlist() {
        if (uid == null) { isLoading = false; return }
        isLoading = true
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .collection("watchlist")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { docs ->
                watchedSearches = docs.documents.mapNotNull { doc ->
                    val d = doc.data ?: return@mapNotNull null
                    WatchedSearch(
                        id = doc.id,
                        query = d["query"] as? String ?: "",
                        lastCheckedTimestamp = d["lastCheckedTimestamp"] as? Long ?: 0L,
                        lastKnownCount = (d["lastKnownCount"] as? Long)?.toInt() ?: 0,
                        lastKnownLowestPrice = d["lastKnownLowestPrice"] as? String ?: "",
                        createdAt = d["createdAt"] as? Long ?: 0L
                    )
                }
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    LaunchedEffect(uid) { loadWatchlist() }

    fun checkForUpdates(search: WatchedSearch) {
        scope.launch {
            checkingId = search.id
            try {
                val results = EbayRepository.searchListings(search.query, sortBy = "newlyListed")
                liveResultsMap = liveResultsMap + (search.id to results)
                val newLowest = results.minByOrNull { it.price.toDoubleOrNull() ?: Double.MAX_VALUE }?.price ?: search.lastKnownLowestPrice
                if (uid != null) {
                    FirebaseFirestore.getInstance().collection("users").document(uid)
                        .collection("watchlist").document(search.id)
                        .update(mapOf(
                            "lastCheckedTimestamp" to System.currentTimeMillis(),
                            "lastKnownCount" to results.size,
                            "lastKnownLowestPrice" to newLowest
                        ))
                }
                watchedSearches = watchedSearches.map {
                    if (it.id == search.id) it.copy(lastCheckedTimestamp = System.currentTimeMillis(), lastKnownCount = results.size, lastKnownLowestPrice = newLowest)
                    else it
                }
            } catch (e: Exception) { }
            checkingId = null
        }
    }

    fun removeFromWatchlist(search: WatchedSearch) {
        if (uid == null) return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("watchlist").document(search.id).delete()
        watchedSearches = watchedSearches.filter { it.id != search.id }
    }

    if (selectedEbayListing != null) {
        EbayListingDetailScreen(
            listing = selectedEbayListing!!,
            onBack = { selectedEbayListing = null },
            onOpenEbay = {
                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(selectedEbayListing!!.itemUrl))) } catch (e: Exception) { }
            }
        )
        return
    }

    when {
        currentUser == null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("📌", fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("SIGN IN TO SAVE SEARCHES", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Bookmark searches in FIND IT to track them here", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
        }
        isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CGreenDeep, modifier = Modifier.size(40.dp))
            }
        }
        watchedSearches.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("📌", fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("NO SAVED SEARCHES", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tap the bookmark icon on any search in FIND IT to track it here", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 19.sp)
                }
            }
        }
        else -> {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Text("${watchedSearches.size} saved search${if (watchedSearches.size != 1) "es" else ""}", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 13.sp)
                }
                items(watchedSearches, key = { it.id }) { search ->
                    WatchedSearchCard(
                        search = search,
                        neonAlpha = neonAlpha,
                        isChecking = checkingId == search.id,
                        liveResults = liveResultsMap[search.id],
                        onCheck = { checkForUpdates(search) },
                        onRemove = { removeFromWatchlist(search) },
                        onListingTap = { selectedEbayListing = it }
                    )
                }
            }
        }
    }
}

@Composable
fun WatchedSearchCard(
    search: WatchedSearch,
    neonAlpha: Float,
    isChecking: Boolean,
    liveResults: List<EbayListing>?,
    onCheck: () -> Unit,
    onRemove: () -> Unit,
    onListingTap: (EbayListing) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val hasNewListings = liveResults != null && liveResults.size > search.lastKnownCount

    val lastCheckedStr = remember(search.lastCheckedTimestamp) {
        if (search.lastCheckedTimestamp > 0L) {
            val diff = System.currentTimeMillis() - search.lastCheckedTimestamp
            when {
                diff < 60_000L -> "just now"
                diff < 3_600_000L -> "${diff / 60_000L}m ago"
                diff < 86_400_000L -> "${diff / 3_600_000L}h ago"
                else -> "${diff / 86_400_000L}d ago"
            }
        } else "never checked"
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ScrapbookDark.copy(alpha = 0.12f))
        )
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ComicGlassBg)
                .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(14.dp))
        ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(search.query, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (hasNewListings) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(CGreen).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("NEW!", fontFamily = BangersFontFamily, color = Color.White, fontSize = 9.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Checked $lastCheckedStr", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape)
                        .background(CAcRed.copy(alpha = 0.1f))
                        .clickable { onRemove() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove", tint = CAcRed, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (search.lastKnownCount > 0) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.92f)).border(1.dp, ScrapbookBorder.copy(alpha = 0.3f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("${search.lastKnownCount} listings", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 11.sp)
                    }
                }
                if (search.lastKnownLowestPrice.isNotBlank()) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(CGreen.copy(alpha = 0.1f)).border(2.5.dp, ScrapbookDark, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        // Green stripe
                        Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                            .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
                        Text("from $${search.lastKnownLowestPrice}", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = CGreen, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ScrapbookDark)
                        .border(2.5.dp, ScrapbookDark, RoundedCornerShape(8.dp))
                        .clickable(enabled = !isChecking) { onCheck(); expanded = true }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(color = MarketOrange, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    } else {
                        Text("🔍 HUNT NOW", fontFamily = BangersFontFamily, color = MarketOrange, fontSize = 12.sp)
                    }
                }
            }

            if (expanded && liveResults != null) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(8.dp))
                if (liveResults.isEmpty()) {
                    Text("No listings found right now", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                } else {
                    liveResults.take(3).forEach { listing ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onListingTap(listing) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.92f))) {
                                if (listing.imageUrl.isNotBlank()) {
                                    AsyncImage(model = listing.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(listing.imageUrl).fillMaxSize())
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(listing.title, fontFamily = NunitoFontFamily, color = ScrapbookDark, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$${listing.price}", fontFamily = BangersFontFamily, color = EbayRed, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
}

// ─── Market Result Card (Amazon, Facebook, Mercari, Etsy) ─────────────────────

@Composable
fun MarketResultCard(market: RetroMarketResult, query: String, neonAlpha: Float, context: android.content.Context) {
    var pressed by remember { mutableStateOf(false) }
    val glowAlpha by rememberGlowPhase(0.4f)
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "mktPress_${market.name}")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "mktShadow_${market.name}")

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)) {
        Box(modifier = Modifier.matchParentSize().offset(x = 6.dp, y = 6.dp).clip(RoundedCornerShape(14.dp)).background(CGreen.copy(alpha = glowAlpha * 0.28f)))
        Box(modifier = Modifier.matchParentSize().offset(x = shadowOff.dp, y = shadowOff.dp).clip(RoundedCornerShape(14.dp)).background(CGreen))
        Box(modifier = Modifier.matchParentSize().padding(3.dp).blur(10.dp).background(market.color.copy(alpha = neonAlpha * 0.12f), RoundedCornerShape(14.dp)))
        Box(
            modifier = Modifier.fillMaxWidth()
                .offset(x = pressAnim.dp, y = pressAnim.dp)
                .clip(RoundedCornerShape(14.dp)).background(ComicGlassBg)
                .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(14.dp))
                .clickable { pressed = true; try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(market.buildUrl(query)))) } catch (e: Exception) { } }
                .padding(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).border(1.dp, market.color.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(painter = painterResource(id = market.logoResId), contentDescription = market.name, modifier = Modifier.size(36.dp).padding(2.dp), contentScale = ContentScale.Fit)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(market.name.uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
                    Text("Search \"$query\" on ${market.name}", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("LOOSE", "CIB", "SEALED").forEach { cond ->
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(market.color.copy(alpha = 0.1f)).border(1.dp, market.color.copy(alpha = 0.3f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(cond, fontFamily = BangersFontFamily, color = market.color, fontSize = 9.sp)
                            }
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(market.color).padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text("SEARCH →", fontFamily = BangersFontFamily, color = Color.White, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Opens browser", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.45f), fontSize = 9.sp)
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Buying Tips Card ─────────────────────────────────────────────────────────

@Composable
fun BuyingTipsCard(query: String, neonAlpha: Float) {
    val tips = remember(query) {
        val q = query.lowercase()
        when {
            q.contains("nes") || q.contains("nintendo entertainment") ->
                listOf("Check the board number stamped on the back","Look for the Nintendo quality seal","Avoid grey-market reproductions — check contacts","Battery saves degrade: test before buying")
            q.contains("snes") || q.contains("super nintendo") ->
                listOf("PAL vs NTSC cartridges are not interchangeable","Check the 16 pin connector for corrosion","CIB SNES games with map inserts are rarer","Super FX chip games fetch a premium")
            q.contains("ps1") || q.contains("playstation 1") ->
                listOf("Black label vs Greatest Hits versions differ in value","Check disc underside for deep scratches","Long box editions are highly collectible","NTSC-J imports often cheaper on modded consoles")
            q.contains("n64") || q.contains("nintendo 64") ->
                listOf("Check for cracked plastic at the cartridge slot","Jumper Pak vs Expansion Pak matters for some games","Pokemon Stadium, Majora's Mask CIB command huge premiums","JP carts need adapter or modified console")
            q.contains("sega") || q.contains("genesis") ->
                listOf("Model 1 vs Model 2 Genesis has different sound chips","32X and Sega CD accessories degrade quickly","Japanese Mega Drive games often cheaper","Check for corrosion in cartridge slot")
            q.contains("sealed") || q.contains("graded") ->
                listOf("WATA and VGA are the two main grading companies","Check grade number — 9.0 vs 9.4 is a huge price gap","Avoid 'factory sealed' without grader verification","H-seam vs Y-fold seams matter for collectors")
            q.contains("arcade") || q.contains("cabinet") ->
                listOf("Check monitor for burn-in before purchasing","Shipping cabinets is expensive — prefer local pickup","PCB boards can often be sourced separately","Jamma harness compatibility is key for multi-game setups")
            else ->
                listOf("Compare prices across all platforms before buying","Search with condition keywords: Loose, CIB, Sealed","Check PriceCharting for the current market value","Buy from sellers with high feedback ratings")
        }
    }

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Box(
            modifier = Modifier.matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ScrapbookDark.copy(alpha = 0.12f))
        )
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ComicGlassBg)
                .border(1.dp, CGreen.copy(alpha = neonAlpha * 0.4f), RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.width(3.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(alpha = neonAlpha)))
                Text("💡 WHAT TO LOOK FOR", fontFamily = BangersFontFamily, color = CGreen, fontSize = 16.sp, letterSpacing = 1.sp)
            }
            tips.forEach { tip ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(5.dp).offset(y = 6.dp).clip(CircleShape).background(CGreen.copy(alpha = 0.6f)))
                    Text(tip, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.7f), fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
        }
    }
}
}

// ─── Price Intelligence Card ──────────────────────────────────────────────────

@Composable
fun PriceIntelligenceCard(query: String, neonAlpha: Float, context: android.content.Context) {
    var pressed by remember { mutableStateOf(false) }
    val glowAlpha by rememberGlowPhase(0.4f)
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "pricePress")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "priceShadow")

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Box(modifier = Modifier.matchParentSize().offset(x = 6.dp, y = 6.dp).clip(RoundedCornerShape(14.dp)).background(CGreen.copy(alpha = glowAlpha * 0.28f)))
        Box(modifier = Modifier.matchParentSize().offset(x = shadowOff.dp, y = shadowOff.dp).clip(RoundedCornerShape(14.dp)).background(CGreen))
        Box(
            modifier = Modifier.fillMaxWidth()
                .offset(x = pressAnim.dp, y = pressAnim.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ComicGlassBg)
                .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(14.dp))
                .clickable {
                    pressed = true
                    val url = "https://www.pricecharting.com/search-products?q=${Uri.encode(query)}"
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) { }
                }
                .padding(16.dp)
        ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(CGreen.copy(alpha = 0.15f)).border(2.5.dp, ScrapbookDark, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { Text("📊", fontSize = 26.sp) }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("MARKET PULSE", fontFamily = BangersFontFamily, color = CGreen, fontSize = 18.sp, letterSpacing = 1.sp)
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(CGreen.copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("PriceCharting", fontFamily = BangersFontFamily, color = CGreen, fontSize = 9.sp)
                    }
                }
                Text("Check Loose / CIB / Sealed prices", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("LOOSE", "CIB", "SEALED").forEach { tier ->
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(CGreen.copy(alpha = 0.12f)).border(2.5.dp, ScrapbookDark, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text(tier, fontFamily = BangersFontFamily, color = CGreen, fontSize = 10.sp)
                        }
                    }
                }
            }
            Text("→", fontFamily = BangersFontFamily, color = CGreen.copy(alpha = neonAlpha), fontSize = 22.sp)
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}
}

// ─── Community Find Card ──────────────────────────────────────────────────────

@Composable
fun CommunityFindCard(find: CommunityFind, neonAlpha: Float, index: Int) {
    val rotation = remember(index) { listOf(-1.5f, 0f, 1.5f, -0.8f, 0.8f)[index % 5] }
    val sourceColor = when (find.source.lowercase()) {
        "ebay" -> EbayRed
        "amazon" -> AmazonOrange
        "facebook", "facebook marketplace" -> FbBlue
        "mercari" -> MercariRed
        "etsy" -> EtsyOrange
        else -> CGreenDeep
    }
    val dateStr = remember(find.timestamp) {
        if (find.timestamp > 0L) SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(find.timestamp)) else ""
    }

    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
            .graphicsLayer { rotationZ = rotation }
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White)
                .border(1.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
        ) {
            Column {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp).background(Color.White.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (find.imageUrl.isNotBlank()) {
                        AsyncImage(model = find.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(find.imageUrl).fillMaxSize())
                    } else {
                        Text("🏆", fontSize = 52.sp)
                    }
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                            .graphicsLayer { rotationZ = 8f }
                            .clip(RoundedCornerShape(4.dp))
                            .background(sourceColor)
                            .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(find.source.uppercase(), fontFamily = BangersFontFamily, color = Color.White, fontSize = 9.sp)
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(find.itemName, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (find.pricePaid.isNotBlank()) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(CGreen.copy(alpha = 0.12f)).border(2.5.dp, ScrapbookDark, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(find.pricePaid, fontFamily = BangersFontFamily, color = CGreen, fontSize = 13.sp)
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)).border(1.dp, ScrapbookBorder, CircleShape), contentAlignment = Alignment.Center) {
                                if (find.profilePicUrl.isNotBlank()) {
                                    // Green stripe
                                    Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                                        .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
                                    AsyncImage(model = find.profilePicUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(find.profilePicUrl).fillMaxSize())
                                } else {
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = ScrapbookDark.copy(alpha = 0.4f), modifier = Modifier.size(10.dp))
                                }
                            }
                            Text(find.username, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            if (dateStr.isNotBlank()) Text("· $dateStr", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
                        }
                    }

                    if (find.note.isNotBlank()) {
                        Text("\"${find.note}\"", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 15.sp)
                    }
                }
            }
        }
    }
}

// ─── Community Find Post Dialog ───────────────────────────────────────────────

@Composable
fun CommunityFindPostDialog(onDismiss: () -> Unit, onPosted: (CommunityFind) -> Unit) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    var itemName       by remember { mutableStateOf("") }
    var pricePaid      by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("eBay") }
    var note           by remember { mutableStateOf("") }
    var imageUri       by remember { mutableStateOf<Uri?>(null) }
    var isSaving       by remember { mutableStateOf(false) }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> imageUri = uri }
    val sources = listOf("eBay", "Amazon", "Facebook", "Mercari", "Etsy", "Other")

    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.comicPop().fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ComicGlassBg).border(2.dp, ScrapbookBorder, RoundedCornerShape(16.dp))) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("🏆 POST A FIND", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Close", tint = ScrapbookDark) }
                    }
                    Text("Share a deal you found with the community!", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                }
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp)).clickable { imageLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUri != null) {
                            AsyncImage(model = imageUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(imageUri).fillMaxSize())
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = ScrapbookDark.copy(alpha = 0.4f), modifier = Modifier.size(32.dp))
                                Text("ADD PHOTO (optional)", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.55f), fontSize = 12.sp)
                            }
                        }
                    }
                }
                item { ScrapbookInputField(value = itemName, onValueChange = { itemName = it }, label = "WHAT DID YOU FIND? *") }
                item { ScrapbookInputField(value = pricePaid, onValueChange = { pricePaid = it }, label = "PRICE PAID (e.g. \$12)") }
                item {
                    Text("WHERE DID YOU FIND IT?", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        sources.forEach { src ->
                            val isSelected = selectedSource == src
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) ScrapbookDark else Color.White.copy(alpha = 0.46f))
                                    .border(2.dp, ScrapbookBorder, RoundedCornerShape(20.dp))
                                    .clickable { selectedSource = src }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(src, fontFamily = BangersFontFamily, color = if (isSelected) Color.White else ScrapbookDark.copy(alpha = 0.6f), fontSize = 12.sp)
                            }
                        }
                    }
                }
                item { ScrapbookInputField(value = note, onValueChange = { note = it }, label = "ADD A NOTE (optional)") }
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (itemName.isNotBlank() && !isSaving) ScrapbookDark else ScrapbookDark.copy(alpha = 0.3f))
                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp))
                            .clickable(enabled = itemName.isNotBlank() && !isSaving) {
                                isSaving = true
                                val uid = currentUser?.uid ?: return@clickable
                                val findId = System.currentTimeMillis().toString()
                                fun saveFind(imgUrl: String) {
                                    FirebaseFirestore.getInstance().collection("community_finds").document(findId)
                                        .set(hashMapOf("uid" to uid, "username" to (currentUser.displayName ?: ""), "profilePicUrl" to (currentUser.photoUrl?.toString() ?: ""), "itemName" to itemName, "pricePaid" to pricePaid, "source" to selectedSource, "imageUrl" to imgUrl, "note" to note, "timestamp" to System.currentTimeMillis()))
                                        .addOnSuccessListener { onPosted(CommunityFind(id = findId, uid = uid, username = currentUser.displayName ?: "", profilePicUrl = currentUser.photoUrl?.toString() ?: "", itemName = itemName, pricePaid = pricePaid, source = selectedSource, imageUrl = imgUrl, note = note, timestamp = System.currentTimeMillis())) }
                                        .addOnFailureListener { isSaving = false }
                                }
                                if (imageUri != null) {
                                    val ref = FirebaseStorage.getInstance().reference.child("community_finds/$findId.jpg")
                                    ref.putFile(imageUri!!).addOnSuccessListener { ref.downloadUrl.addOnSuccessListener { url -> saveFind(url.toString()) } }.addOnFailureListener { saveFind("") }
                                } else { saveFind("") }
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSaving) CircularProgressIndicator(color = CGreen, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text("POST FIND 🏆", fontFamily = BangersFontFamily, color = CGreen, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

// ─── Community Tab ────────────────────────────────────────────────────────────

@Composable
fun CommunityTab(
    listings: List<MarketplaceListing>,
    isLoading: Boolean,
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    onListingClick: (MarketplaceListing) -> Unit,
    neonAlpha: Float
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listingTypes) { type ->
                val isSelected = selectedType == type
                var pressed by remember { mutableStateOf(false) }
                val chipScale by animateFloatAsState(targetValue = if (pressed) 0.92f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "typeChip_$type")
                Box(
                    modifier = Modifier.scale(chipScale).clip(RoundedCornerShape(20.dp))
                        .background(when { isSelected && type == "FOR SALE" -> CGreen; isSelected && type == "FOR TRADE" -> CAcBlue; isSelected && type == "WANTED" -> CAcRed; isSelected -> CGreen; else -> Color.White.copy(alpha = 0.46f) })
                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(20.dp))
                        .clickable { pressed = true; onTypeSelected(type) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(type, fontFamily = BangersFontFamily, color = if (isSelected) ScrapbookDark else ScrapbookDark.copy(alpha = 0.6f), fontSize = 13.sp)
                }
                LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
            }
        }

        when {
            isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CGreenDeep, modifier = Modifier.size(40.dp))
            }
            listings.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("🛒", fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("NO LISTINGS YET", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Be the first to post!\nTap + in the toolbar to add one.", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
                }
            }
            else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                item {
                    Text("${listings.size} listing${if (listings.size != 1) "s" else ""}", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 13.sp)
                }
                items(listings, key = { it.id }) { listing ->
                    ListingCard(listing = listing, onClick = { onListingClick(listing) })
                }
            }
        }
    }
}

// ─── Preserved original composables ──────────────────────────────────────────

@Composable
fun ListingCard(listing: MarketplaceListing, onClick: () -> Unit) {
    val typeColor = when (listing.type) { "FOR SALE" -> CGreen; "FOR TRADE" -> CAcBlue; "WANTED" -> CAcRed; else -> ScrapbookDark.copy(alpha = 0.7f) }
    ScrapbookCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }, backgroundColor = ComicGlassBg, cornerRadius = 14.dp, shadowOffset = 4.dp) {
        Row(modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier.size(90.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                if (listing.imageUrl.isNotBlank()) AsyncImage(model = listing.imageUrl, contentDescription = listing.title, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(listing.imageUrl).fillMaxSize())
                else Text("🎮", fontSize = 32.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(typeColor).padding(horizontal = 8.dp, vertical = 2.dp)) { Text(listing.type, fontFamily = BangersFontFamily, color = Color.White, fontSize = 10.sp) }
                    if (listing.paypalUsername.isNotBlank() && listing.type == "FOR SALE") {
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(PayPalBlue).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("PayPal", fontFamily = BangersFontFamily, color = Color.White, fontSize = 10.sp) }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(listing.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 22.sp)
                if (listing.platform.isNotBlank()) Text(listing.platform, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (listing.price.isNotBlank() && listing.type == "FOR SALE") Box(
                            modifier = Modifier.swingIn(listing.title)
                                .clip(RoundedCornerShape(8.dp)).background(CAcYellowL)
                                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(8.dp))
                                .padding(start = 8.dp, end = 12.dp, top = 3.dp, bottom = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // string hole
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color.White).border(1.5.dp, ScrapbookDark, CircleShape))
                                Text(listing.price, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 17.sp)
                            }
                        }
                    if (listing.condition.isNotBlank()) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.92f)).border(1.dp, ScrapbookBorder.copy(alpha = 0.3f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(listing.condition, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 10.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)).border(1.dp, ScrapbookBorder, CircleShape), contentAlignment = Alignment.Center) {
                        if (listing.sellerProfilePicUrl.isNotBlank()) AsyncImage(model = listing.sellerProfilePicUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(listing.sellerProfilePicUrl).fillMaxSize())
                        else Icon(Icons.Filled.Person, contentDescription = null, tint = ScrapbookDark.copy(alpha = 0.4f), modifier = Modifier.size(12.dp))
                    }
                    Text(listing.sellerUsername, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun PayPalConfirmDialog(listing: MarketplaceListing, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.comicPop().fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ComicGlassBg).border(2.dp, ScrapbookBorder, RoundedCornerShape(16.dp))) {
            Column(modifier = Modifier.padding(20.dp)) {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(PayPalBlue).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text("Pay with PayPal", fontFamily = BangersFontFamily, color = Color.White, fontSize = 22.sp, letterSpacing = 1.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("ORDER SUMMARY", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.92f)).border(1.dp, ScrapbookBorder.copy(alpha = 0.3f), RoundedCornerShape(10.dp)).padding(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Item", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
                            Text(listing.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp), textAlign = TextAlign.End)
                        }
                        if (listing.platform.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Platform", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
                                Text(listing.platform, fontFamily = NunitoFontFamily, color = ScrapbookDark, fontSize = 13.sp)
                            }
                        }
                        if (listing.condition.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Condition", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
                                Text(listing.condition, fontFamily = NunitoFontFamily, color = ScrapbookDark, fontSize = 13.sp)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Seller", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
                            Text(listing.sellerUsername, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                        }
                        HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.2f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Total", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                            Text(listing.price, fontFamily = BangersFontFamily, color = CGreen, fontSize = 22.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(PayPalBlue.copy(alpha = 0.08f)).border(1.dp, PayPalBlue.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(10.dp)) {
                    Text("🔒 You'll be taken to PayPal to complete payment safely. RetroHub never handles your payment details.", fontFamily = NunitoFontFamily, color = PayPalBlue, fontSize = 11.sp, lineHeight = 16.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(PayPalBlue).border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp)).clickable { onConfirm() }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                    Text("CONTINUE TO PAYPAL →", fontFamily = BangersFontFamily, color = Color.White, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp)).clickable { onDismiss() }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text("CANCEL", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun ListingDetailScreen(
    listing: MarketplaceListing, currentUid: String,
    chatViewModel: ChatViewModel, authViewModel: AuthViewModel,
    onBack: () -> Unit, onViewSeller: (String) -> Unit
) {
    val context = LocalContext.current
    val myProfile by authViewModel.userProfile.collectAsState()
    var isStartingChat by remember { mutableStateOf(false) }
    var showPayPalDialog by remember { mutableStateOf(false) }
    val typeColor = when (listing.type) { "FOR SALE" -> CGreen; "FOR TRADE" -> CAcBlue; "WANTED" -> CAcRed; else -> ScrapbookDark.copy(alpha = 0.7f) }
    val dateStr = remember(listing.timestamp) { if (listing.timestamp > 0L) SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(listing.timestamp)) else "" }

    if (showPayPalDialog) {
        PayPalConfirmDialog(listing = listing, onConfirm = {
            showPayPalDialog = false
            val cleanPrice = listing.price.replace("$", "").replace(" ", "").trim()
            val paypalUrl = "https://www.paypal.com/paypalme/${listing.paypalUsername}/$cleanPrice"
            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(paypalUrl))) } catch (e: Exception) { }
        }, onDismiss = { showPayPalDialog = false })
    }

    Box(modifier = Modifier.fillMaxSize().background(ComicGlassBg)) {
        HalftoneBackground(modifier = Modifier.fillMaxSize())
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                    if (listing.imageUrl.isNotBlank()) AsyncImage(model = listing.imageUrl, contentDescription = listing.title, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(listing.imageUrl).fillMaxSize())
                    else Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) { Text("🎮", fontSize = 72.sp) }
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(top = 40.dp, start = 8.dp).clip(CircleShape).background(CGreen).border(2.dp, ScrapbookBorder, CircleShape).clickable { onBack() }.padding(8.dp)) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 12.dp).clip(RoundedCornerShape(8.dp)).background(typeColor).border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(listing.type, fontFamily = BangersFontFamily, color = Color.White, fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(listing.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 28.sp, lineHeight = 32.sp)
                    if (listing.price.isNotBlank() && listing.type == "FOR SALE") {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(listing.price, fontFamily = BangersFontFamily, color = CGreen, fontSize = 24.sp)
                            if (listing.paypalUsername.isNotBlank()) {
                                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(PayPalBlue).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                    Text("PayPal accepted", fontFamily = BangersFontFamily, color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ComicGlassBg, cornerRadius = 12.dp) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (listing.platform.isNotBlank()) GameDetailRow(label = "Platform", value = listing.platform)
                            if (listing.condition.isNotBlank()) GameDetailRow(label = "Condition", value = listing.condition)
                            if (dateStr.isNotBlank()) GameDetailRow(label = "Listed", value = dateStr)
                        }
                    }
                    if (listing.description.isNotBlank()) {
                        Text("DESCRIPTION", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp)
                        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ComicGlassBg, cornerRadius = 12.dp) {
                            Text(listing.description, fontFamily = NunitoFontFamily, color = ScrapbookDark, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(16.dp))
                        }
                    }
                    if (listing.type == "FOR SALE" && listing.paypalUsername.isNotBlank() && listing.sellerUid != currentUid) {
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(PayPalBlue).border(2.dp, ScrapbookBorder, RoundedCornerShape(12.dp)).clickable { showPayPalDialog = true }.padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("BUY NOW WITH PAYPAL", fontFamily = BangersFontFamily, color = Color.White, fontSize = 20.sp, letterSpacing = 1.sp)
                                Text("Safe & secure payment", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            }
                        }
                    }
                    if (listing.sellerUid != currentUid) {
                        Text("SELLER", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp)
                        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ComicGlassBg, cornerRadius = 12.dp) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth().clickable { onViewSeller(listing.sellerUid) }, verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookBorder, CircleShape), contentAlignment = Alignment.Center) {
                                        if (listing.sellerProfilePicUrl.isNotBlank()) AsyncImage(model = listing.sellerProfilePicUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(listing.sellerProfilePicUrl).fillMaxSize())
                                        else Icon(Icons.Filled.Person, contentDescription = null, tint = ScrapbookDark.copy(alpha = 0.4f), modifier = Modifier.size(24.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(listing.sellerUsername.uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp)
                                        Text("Tap to view profile →", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp))
                                        .clickable(enabled = !isStartingChat) {
                                            isStartingChat = true
                                            val sellerData = UserProfileData(uid = listing.sellerUid, username = listing.sellerUsername, profilePictureUrl = listing.sellerProfilePicUrl, userHandle = "", bio = "", email = "", setupComplete = true, topGames = emptyList(), topSoundtracks = emptyList())
                                            val myProfileData = myProfile?.let { UserProfileData(uid = currentUid, username = it.username, profilePictureUrl = it.profilePictureUrl ?: "", userHandle = it.userHandle, bio = it.bio, email = it.email, setupComplete = it.setupComplete, topGames = it.topGames, topSoundtracks = it.topSoundtracks, bannerUrl = it.bannerUrl) }
                                            chatViewModel.getOrCreateDm(otherUser = sellerData, myProfile = myProfileData, onResult = { isStartingChat = false })
                                        }
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isStartingChat) CircularProgressIndicator(color = CGreen, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Filled.Chat, contentDescription = null, tint = CGreenDeep, modifier = Modifier.size(18.dp))
                                        Text("MESSAGE SELLER", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 18.sp)
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
    sellerUid: String, sellerUsername: String, sellerProfilePicUrl: String,
    onDismiss: () -> Unit, onSaved: (MarketplaceListing) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var paypalUsername by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("FOR SALE") }
    var selectedCondition by remember { mutableStateOf("GOOD") }
    var selectedPlatform by remember { mutableStateOf("ANY") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> imageUri = uri }

    Box(modifier = Modifier.fillMaxSize().background(ComicGlassBg)) {
        HalftoneBackground(modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().background(CGreen).border(BorderStroke(2.dp, ScrapbookBorder)).padding(top = 16.dp, bottom = 12.dp, start = 4.dp, end = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White) }
                    Text("NEW LISTING", fontFamily = BangersFontFamily, color = Color.White, fontSize = 26.sp, modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(if (title.isNotBlank() && !isSaving) ScrapbookDark else ScrapbookDark.copy(alpha = 0.3f))
                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                            .clickable(enabled = title.isNotBlank() && !isSaving) {
                                isSaving = true; errorMsg = ""
                                val listingId = System.currentTimeMillis().toString()
                                val listingData = hashMapOf<String, Any>("title" to title, "description" to description, "price" to price, "type" to selectedType, "condition" to selectedCondition, "platform" to selectedPlatform, "sellerUid" to sellerUid, "sellerUsername" to sellerUsername, "sellerProfilePicUrl" to sellerProfilePicUrl, "timestamp" to System.currentTimeMillis(), "imageUrl" to "", "paypalUsername" to paypalUsername.trim())
                                fun saveListing(imageUrl: String) {
                                    listingData["imageUrl"] = imageUrl
                                    FirebaseFirestore.getInstance().collection("marketplace").document(listingId).set(listingData)
                                        .addOnSuccessListener { onSaved(MarketplaceListing(id = listingId, title = title, description = description, price = price, type = selectedType, condition = selectedCondition, platform = selectedPlatform, sellerUid = sellerUid, sellerUsername = sellerUsername, sellerProfilePicUrl = sellerProfilePicUrl, timestamp = System.currentTimeMillis(), imageUrl = imageUrl, paypalUsername = paypalUsername.trim())) }
                                        .addOnFailureListener { isSaving = false; errorMsg = "Failed to save listing" }
                                }
                                if (imageUri != null) {
                                    val ref = FirebaseStorage.getInstance().reference.child("marketplace/$listingId.jpg")
                                    ref.putFile(imageUri!!).addOnSuccessListener { ref.downloadUrl.addOnSuccessListener { url -> saveListing(url.toString()) } }.addOnFailureListener { saveListing("") }
                                } else { saveListing("") }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        if (isSaving) CircularProgressIndicator(color = CGreen, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text("POST", fontFamily = BangersFontFamily, color = CGreen, fontSize = 16.sp)
                    }
                }
            }
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (errorMsg.isNotBlank()) {
                    item { Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(CAcRed.copy(alpha = 0.1f)).border(1.dp, CAcRed, RoundedCornerShape(8.dp)).padding(12.dp)) { Text(errorMsg, fontFamily = NunitoFontFamily, color = CAcRed, fontSize = 13.sp) } }
                }
                item {
                    Text("PHOTO", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookBorder, RoundedCornerShape(12.dp)).clickable { imageLauncher.launch("image/*") }, contentAlignment = Alignment.Center) {
                        if (imageUri != null) {
                            AsyncImage(model = imageUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(imageUri).fillMaxSize())
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) { Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = ScrapbookDark.copy(alpha = 0.4f), modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("TAP TO ADD PHOTO", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.55f), fontSize = 14.sp)
                            }
                        }
                    }
                }
                item {
                    Text("LISTING TYPE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("FOR SALE", "FOR TRADE", "WANTED").forEach { type ->
                            val isSelected = selectedType == type
                            val color = when (type) { "FOR SALE" -> CGreen; "FOR TRADE" -> CAcBlue; else -> CAcRed }
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (isSelected) color else Color.White.copy(alpha = 0.46f)).border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)).clickable { selectedType = type }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(type, fontFamily = BangersFontFamily, color = if (isSelected) Color.White else ScrapbookDark.copy(alpha = 0.6f), fontSize = 13.sp)
                            }
                        }
                    }
                }
                item { ScrapbookInputField(value = title, onValueChange = { title = it }, label = "GAME TITLE *") }
                if (selectedType == "FOR SALE") {
                    item { ScrapbookInputField(value = price, onValueChange = { price = it }, label = "PRICE (e.g. \$15)") }
                    item {
                        Text("PAYPAL", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Add your PayPal.me username so buyers can pay securely", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ComicGlassBg).border(2.dp, PayPalBlue.copy(alpha = 0.5f), RoundedCornerShape(10.dp))) {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(PayPalBlue).padding(horizontal = 8.dp, vertical = 6.dp)) {
                                    Text("paypal.me/", fontFamily = NunitoFontFamily, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(value = paypalUsername, onValueChange = { paypalUsername = it.replace(" ", "").replace("paypal.me/", "") }, placeholder = { Text("yourusername", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 14.sp) }, singleLine = true, textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookDark), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, cursorColor = ScrapbookDark), modifier = Modifier.weight(1f))
                            }
                        }
                        if (paypalUsername.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Buyers will be sent to: paypal.me/$paypalUsername", fontFamily = NunitoFontFamily, color = PayPalBlue, fontSize = 11.sp)
                        }
                    }
                }
                item {
                    Text("PLATFORM", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(gamePlatformsList) { platform ->
                            val isSelected = selectedPlatform == platform
                            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (isSelected) CGreen else Color.White.copy(alpha = 0.46f)).border(2.dp, ScrapbookBorder, RoundedCornerShape(20.dp)).clickable { selectedPlatform = platform }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text(platform, fontFamily = BangersFontFamily, color = if (isSelected) ScrapbookDark else ScrapbookDark.copy(alpha = 0.6f), fontSize = 12.sp)
                            }
                        }
                    }
                }
                item {
                    Text("CONDITION", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        gameConditions.forEach { condition ->
                            val isSelected = selectedCondition == condition
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (isSelected) CGreen else Color.White.copy(alpha = 0.46f)).border(2.dp, CGreenDeep.copy(alpha=0.3f), RoundedCornerShape(8.dp)).clickable { selectedCondition = condition }.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                Text(condition, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp)
                            }
                        }
                    }
                }
                item { ScrapbookInputField(value = description, onValueChange = { description = it }, label = "DESCRIPTION") }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}
package com.example.hubretro

import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.*
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// ─── Data ─────────────────────────────────────────────────────────────────────

data class RetroEvent(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val date: Long = 0L,
    val type: String = "COMMUNITY",
    val authorUid: String = "",
    val authorUsername: String = "",
    val emoji: String = "🎮",
    val coverUrl: String? = null
)

// ─── Background refresh worker (weekly) ──────────────────────────────────────

class EventsRefreshWorker(context: android.content.Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val db = FirebaseFirestore.getInstance()

            // Real upcoming games from IGDB (today → 8 months)
            val upcomingGames = IGDBRepository.fetchUpcomingGames()
            upcomingGames.forEach { game ->
                val safeId = "igdb_${game.id}"
                db.collection("auto_events").document(safeId).set(mapOf(
                    "title"          to game.name,
                    "description"    to (game.summary?.take(120) ?: "Upcoming release"),
                    "date"           to (game.releaseDate * 1000L),
                    "type"           to "UPCOMING",
                    "emoji"          to "🎮",
                    "coverUrl"       to (game.coverUrl ?: ""),
                    "authorUid"      to "",
                    "authorUsername" to "RetroHub",
                    "igdbId"         to game.id,
                    "hypes"          to game.hypes,
                    "platforms"      to game.platforms
                ))
            }

            // Real upcoming DLC / expansions
            val dlcs = IGDBRepository.fetchUpcomingDLC()
            dlcs.forEach { game ->
                val safeId = "igdb_dlc_${game.id}"
                db.collection("auto_events").document(safeId).set(mapOf(
                    "title"          to game.name,
                    "description"    to (game.summary?.take(120) ?: "New DLC / Expansion"),
                    "date"           to (game.releaseDate * 1000L),
                    "type"           to "DLC",
                    "emoji"          to "📦",
                    "coverUrl"       to (game.coverUrl ?: ""),
                    "authorUid"      to "",
                    "authorUsername" to "RetroHub",
                    "igdbId"         to game.id,
                    "hypes"          to game.hypes,
                    "platforms"      to game.platforms
                ))
            }
            Result.success()
        } catch (e: Exception) { Result.retry() }
    }
}

fun scheduleEventsRefresh(context: android.content.Context) {
    val request = PeriodicWorkRequestBuilder<EventsRefreshWorker>(7, TimeUnit.DAYS)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "events_refresh", ExistingPeriodicWorkPolicy.KEEP, request
    )
}

// ─── Curated data (conventions, esports, anniversaries) — updated for 2026 ───

val gamingConventions = listOf(
    RetroEvent(id = "con1",  title = "Summer Game Fest 2026",     description = "Geoff Keighley's showcase with world premieres.",                date = dateOf(6,  6, 2026),  type = "CONVENTION", emoji = "🎪"),
    RetroEvent(id = "con2",  title = "Gamescom 2026",             description = "Europe's largest gaming convention in Cologne, Germany.",        date = dateOf(8, 19, 2026),  type = "CONVENTION", emoji = "🇩🇪"),
    RetroEvent(id = "con3",  title = "Tokyo Game Show 2026",      description = "Japan's premier gaming expo showcasing Japanese developers.",    date = dateOf(9, 24, 2026),  type = "CONVENTION", emoji = "🇯🇵"),
    RetroEvent(id = "con4",  title = "PAX East 2026",             description = "The East Coast gaming convention in Boston.",                    date = dateOf(3, 26, 2026),  type = "CONVENTION", emoji = "🎮"),
    RetroEvent(id = "con5",  title = "PAX West 2026",             description = "The original PAX returns to Seattle.",                           date = dateOf(8, 28, 2026),  type = "CONVENTION", emoji = "🌲"),
    RetroEvent(id = "con6",  title = "Nintendo Direct — Q3",      description = "Nintendo's showcase with major Switch 2 announcements.",         date = dateOf(9,  4, 2026),  type = "CONVENTION", emoji = "🍄"),
    RetroEvent(id = "con7",  title = "PlayStation State of Play", description = "Sony's showcase featuring PS exclusives and third-party titles.",date = dateOf(9, 10, 2026),  type = "CONVENTION", emoji = "🎯"),
    RetroEvent(id = "con8",  title = "Xbox Games Showcase",       description = "Microsoft's annual showcase with Game Pass titles.",             date = dateOf(6,  7, 2026),  type = "CONVENTION", emoji = "💚"),
    RetroEvent(id = "con9",  title = "The Game Awards 2026",      description = "Gaming's biggest night — world premieres and GOTY.",             date = dateOf(12,10, 2026),  type = "CONVENTION", emoji = "🏆"),
    RetroEvent(id = "con10", title = "EVO 2026",                  description = "The Evolution Championship Series for fighting games.",          date = dateOf(8,  7, 2026),  type = "CONVENTION", emoji = "🥊")
)

val esportsEvents = listOf(
    RetroEvent(id = "esp1", title = "LoL World Championship",      description = "The pinnacle of professional League of Legends.",               date = dateOf(10,25, 2026), type = "ESPORTS", emoji = "⚡"),
    RetroEvent(id = "esp2", title = "The International — Dota 2", description = "Valve's annual Dota 2 world championship.",                      date = dateOf(9,  5, 2026), type = "ESPORTS", emoji = "🌐"),
    RetroEvent(id = "esp3", title = "CS2 Major — Copenhagen",     description = "One of the biggest Counter-Strike 2 tournaments of the year.",   date = dateOf(8, 20, 2026), type = "ESPORTS", emoji = "💥"),
    RetroEvent(id = "esp4", title = "Valorant Champions 2026",    description = "Riot's flagship Valorant world championship event.",             date = dateOf(8,  3, 2026), type = "ESPORTS", emoji = "🎯"),
    RetroEvent(id = "esp5", title = "Overwatch World Cup 2026",   description = "Nations compete in Blizzard's team-based hero shooter.",        date = dateOf(11, 1, 2026), type = "ESPORTS", emoji = "🦸"),
    RetroEvent(id = "esp6", title = "Rocket League World Champ.", description = "Psyonix's rocket-powered car soccer world finals.",              date = dateOf(6, 20, 2026), type = "ESPORTS", emoji = "🚗"),
    RetroEvent(id = "esp7", title = "Street Fighter 6 Capcom Cup",description = "The premier Street Fighter competitive circuit.",               date = dateOf(10,10, 2026), type = "ESPORTS", emoji = "🥋"),
    RetroEvent(id = "esp8", title = "Fortnite World Cup 2026",    description = "Epic's global Fortnite competitive season grand finals.",        date = dateOf(9, 12, 2026), type = "ESPORTS", emoji = "🏆")
)

val retroAnniversaries = listOf(
    RetroEvent(id = "ann1",  title = "Super Mario Bros — 41 Years",   description = "Super Mario Bros released for the NES in Japan (1985).",       date = dateOf(9, 13, 2026),  type = "ANNIVERSARY", emoji = "🍄"),
    RetroEvent(id = "ann2",  title = "Game Boy — 37 Years",           description = "Nintendo's Game Boy released in Japan (April 21, 1989).",       date = dateOf(4, 21, 2026),  type = "ANNIVERSARY", emoji = "🎮"),
    RetroEvent(id = "ann3",  title = "Sonic the Hedgehog — 35 Years", description = "Sonic debuted on the Sega Genesis on June 23, 1991.",           date = dateOf(6, 23, 2026),  type = "ANNIVERSARY", emoji = "💨"),
    RetroEvent(id = "ann4",  title = "PlayStation — 32 Years",        description = "Sony launched the original PlayStation in Japan (Dec 3, 1994).",date = dateOf(12, 3, 2026),  type = "ANNIVERSARY", emoji = "🎯"),
    RetroEvent(id = "ann5",  title = "Zelda: Ocarina of Time — 28Y",  description = "Ocarina of Time released on November 21, 1998.",                date = dateOf(11,21, 2026),  type = "ANNIVERSARY", emoji = "🗡️"),
    RetroEvent(id = "ann6",  title = "Pac-Man — 46 Years",            description = "Pac-Man appeared in arcades in Japan on May 22, 1980.",         date = dateOf(5, 22, 2026),  type = "ANNIVERSARY", emoji = "👾"),
    RetroEvent(id = "ann7",  title = "Tetris — 42 Years",             description = "Tetris released June 6, 1984 by Alexey Pajitnov.",              date = dateOf(6,  6, 2026),  type = "ANNIVERSARY", emoji = "🧩"),
    RetroEvent(id = "ann8",  title = "Doom — 33 Years",               description = "id Software released Doom as shareware on December 10, 1993.",  date = dateOf(12,10, 2026),  type = "ANNIVERSARY", emoji = "🔫"),
    RetroEvent(id = "ann9",  title = "Nintendo NES — 41 Years",       description = "The NES launched in North America on October 18, 1985.",         date = dateOf(10,18, 2026),  type = "ANNIVERSARY", emoji = "🕹️"),
    RetroEvent(id = "ann10", title = "Street Fighter II — 35 Years",  description = "Street Fighter II hit arcades on February 6, 1991.",             date = dateOf(2,  6, 2026),  type = "ANNIVERSARY", emoji = "🥊"),
    RetroEvent(id = "ann11", title = "Pokémon Red & Blue — 28 Years", description = "Pokémon Red and Blue launched in the US on September 28, 1998.",date = dateOf(9, 28, 2026),  type = "ANNIVERSARY", emoji = "⚡"),
    RetroEvent(id = "ann12", title = "Sega Genesis — 37 Years",       description = "The Sega Genesis launched in North America on August 14, 1989.", date = dateOf(8, 14, 2026),  type = "ANNIVERSARY", emoji = "🌀")
)

// ─── Helpers ──────────────────────────────────────────────────────────────────

fun dateOf(month: Int, day: Int, year: Int = Calendar.getInstance().get(Calendar.YEAR)): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, month - 1)
    cal.set(Calendar.DAY_OF_MONTH, day)
    cal.set(Calendar.HOUR_OF_DAY, 12); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
    return cal.timeInMillis
}

fun eventTypeColor(type: String): Color = when (type) {
    "ANNIVERSARY" -> CGreenDeep
    "UPCOMING"    -> CGreen
    "DLC"         -> CAcBlue
    "CONVENTION"  -> CAcRed
    "ESPORTS"     -> CAcPurple
    "COMMUNITY"   -> CAcBlue
    else          -> ScrapbookDark
}

fun eventTypeLabel(type: String): String = when (type) {
    "ANNIVERSARY" -> "🏆 ANNIVERSARY"
    "UPCOMING"    -> "🎮 UPCOMING"
    "DLC"         -> "📦 DLC / UPDATE"
    "CONVENTION"  -> "🎪 CONVENTION"
    "ESPORTS"     -> "🏅 ESPORTS"
    "COMMUNITY"   -> "👥 COMMUNITY"
    else          -> "📌 EVENT"
}

fun countdownText(dateMs: Long): String {
    val diffMs = dateMs - System.currentTimeMillis()
    return when {
        diffMs < -86_400_000L -> "RELEASED"
        diffMs < 0            -> "TODAY!"
        diffMs < 86_400_000L  -> "TODAY!"
        diffMs < 172_800_000L -> "TOMORROW"
        else                  -> "${(diffMs / 86_400_000L).toInt()} DAYS"
    }
}

fun countdownForEpochSec(epochSec: Long): String = countdownText(epochSec * 1000L)

/** Badge text for a release, based on IGDB game_type and whether it's already out. */
fun releaseTypeLabel(gameType: Int, releaseEpochSec: Long): String {
    val out = releaseEpochSec * 1000L <= System.currentTimeMillis()
    val kind = when (gameType) {
        1 -> "DLC"; 2 -> "EXPANSION"; 4 -> "STANDALONE"; 13 -> "PACK"; 14 -> "UPDATE"
        8 -> "REMAKE"; 9 -> "REMASTER"; 10 -> "EXPANDED"; 11 -> "PORT"
        else -> if (out) "OUT NOW" else "NEW"
    }
    return if (out && gameType != 0 && kind != "OUT NOW") "$kind · OUT" else kind
}

/** Release wishlist persisted on device (read by RetroSyncWorker for "out today" pings). */
object ReleaseWishlist {
    private const val PREFS = "release_wishlist"
    fun load(context: android.content.Context): List<Int> =
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .getStringSet("ids", emptySet())!!.mapNotNull { it.toIntOrNull() }
    fun save(context: android.content.Context, ids: List<Int>) {
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).edit()
            .putStringSet("ids", ids.map { it.toString() }.toSet()).apply()
    }
}

fun platformAbbr(name: String): String? = when {
    name.contains("PlayStation 5",  ignoreCase = true) -> "PS5"
    name.contains("PlayStation 4",  ignoreCase = true) -> "PS4"
    name.contains("Xbox Series",    ignoreCase = true) -> "XBX"
    name.contains("Xbox One",       ignoreCase = true) -> "XB1"
    name.contains("Nintendo Switch",ignoreCase = true) -> "NSW"
    name.contains("Windows",        ignoreCase = true) -> "PC"
    name.equals("PC",               ignoreCase = true) -> "PC"
    name.contains("Mac",            ignoreCase = true) -> "MAC"
    else -> null
}

fun genreAbbr(name: String): String = when {
    name.contains("Role-playing",   ignoreCase = true) -> "RPG"
    name.contains("Hack and slash", ignoreCase = true) -> "HACK & SLASH"
    name.contains("Real Time Strat",ignoreCase = true) -> "RTS"
    name.contains("Turn-based",     ignoreCase = true) -> "TURN-BASED"
    name.contains("Shooter",        ignoreCase = true) -> "SHOOTER"
    name.contains("Adventure",      ignoreCase = true) -> "ADVENTURE"
    name.contains("Platform",       ignoreCase = true) -> "PLATFORM"
    name.contains("Fighting",       ignoreCase = true) -> "FIGHTING"
    name.contains("Strategy",       ignoreCase = true) -> "STRATEGY"
    name.contains("Simulation",     ignoreCase = true) -> "SIM"
    name.contains("Simulator",      ignoreCase = true) -> "SIM"
    name.contains("Racing",         ignoreCase = true) -> "RACING"
    name.contains("Sport",          ignoreCase = true) -> "SPORT"
    name.contains("Puzzle",         ignoreCase = true) -> "PUZZLE"
    name.contains("Action",         ignoreCase = true) -> "ACTION"
    name.contains("Horror",         ignoreCase = true) -> "HORROR"
    name.contains("Stealth",        ignoreCase = true) -> "STEALTH"
    name.contains("Music",          ignoreCase = true) -> "MUSIC"
    else                                               -> name.uppercase().take(10)
}

fun gameModeIcon(mode: String): String = when {
    mode.contains("Single",   ignoreCase = true) -> "🎮"
    mode.contains("Multi",    ignoreCase = true) -> "👥"
    mode.contains("Co-op",    ignoreCase = true) -> "🤝"
    mode.contains("Battle",   ignoreCase = true) -> "⚔️"
    mode.contains("Split",    ignoreCase = true) -> "📺"
    else                                         -> "🕹️"
}

fun esrbColor(rating: String): Color = when (rating) {
    "E", "EC" -> CGreenDeep      // dark green
    "E10+"    -> CAcBlue      // blue
    "T"       -> Color(0xFFE65100)      // orange
    "M"       -> CAcRedD      // red
    "AO"      -> CAcPurple      // dark purple
    else      -> ScrapbookTextMuted      // gray (RP)
}

// ─── IGDB cover cache (for RetroEvent objects, fetches cover by title search) ─

val eventCoverCache = mutableStateMapOf<String, String>()

@Composable
fun rememberEventCover(event: RetroEvent): String? {
    if (event.type != "UPCOMING" && event.type != "DLC") return null
    val scope = rememberCoroutineScope()
    LaunchedEffect(event.id) {
        if (eventCoverCache.containsKey(event.id)) return@LaunchedEffect
        eventCoverCache[event.id] = ""
        scope.launch {
            try {
                val results = IGDBRepository.searchGames(event.title)
                val match = results.firstOrNull { it.coverUrl != null && it.name.equals(event.title, ignoreCase = true) }
                    ?: results.firstOrNull { it.coverUrl != null }
                eventCoverCache[event.id] = match?.coverUrl ?: ""
            } catch (_: Exception) { eventCoverCache[event.id] = "" }
        }
    }
    val cached = eventCoverCache[event.id]
    return if (cached.isNullOrEmpty()) null else cached
}

// ─── Main Screen ───────────────────────────────────────────────────────────────

@Composable
fun EventsScreen(
    authViewModel: AuthViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context      = LocalContext.current
    val currentUser  by authViewModel.currentUser.collectAsState()
    val firebaseProfile by authViewModel.userProfile.collectAsState()

    var communityEvents by remember { mutableStateOf<List<RetroEvent>>(emptyList()) }
    var igdbGames      by remember { mutableStateOf<List<IGDBUpcomingGame>>(emptyList()) }
    var igdbDLCs       by remember { mutableStateOf<List<IGDBUpcomingGame>>(emptyList()) }
    var isLoading      by remember { mutableStateOf(true) }
    var showAddEvent   by remember { mutableStateOf(false) }
    var selectedFilter   by remember { mutableStateOf("ALL") }
    var selectedPlatform by remember { mutableStateOf("ALL") }
    var selectedGenre    by remember { mutableStateOf("ALL") }
    val wishlist = remember { mutableStateListOf<Int>() }
    // Wishlist survives restarts → the sync worker can ping "your game is out today!"
    LaunchedEffect(Unit) {
        if (wishlist.isEmpty()) wishlist.addAll(ReleaseWishlist.load(context))
        snapshotFlow { wishlist.toList() }.collect { ReleaseWishlist.save(context, it) }
    }

    val today = remember { Calendar.getInstance() }
    var displayedMonth by remember { mutableStateOf(today.get(Calendar.MONTH)) }
    var displayedYear  by remember { mutableStateOf(today.get(Calendar.YEAR)) }
    var selectedDay    by remember { mutableStateOf(today.get(Calendar.DAY_OF_MONTH)) }

    val neonAlpha by rememberGlowRange(0.5f, 1f)

    LaunchedEffect(Unit) {
        scheduleEventsRefresh(context)

        // ── Real upcoming games from IGDB (today → 12 months) ────────────────
        try {
            val games = IGDBRepository.fetchUpcomingGames()
            android.util.Log.d("EventsScreen", "IGDB games loaded: ${games.size}")
            igdbGames = games
        } catch (e: Exception) {
            android.util.Log.e("EventsScreen", "fetchUpcomingGames failed: ${e.message}", e)
        }
        try {
            val dlcs = IGDBRepository.fetchUpcomingDLC()
            android.util.Log.d("EventsScreen", "IGDB DLCs loaded: ${dlcs.size}")
            igdbDLCs = dlcs
        } catch (e: Exception) {
            android.util.Log.e("EventsScreen", "fetchUpcomingDLC failed: ${e.message}", e)
        }
        isLoading = false

        // ── Community events from Firestore ───────────────────────────────────
        try {
            val docs = FirebaseFirestore.getInstance()
                .collection("events")
                .orderBy("date", Query.Direction.ASCENDING)
                .limit(50).get().await()
            communityEvents = docs.documents.mapNotNull { doc ->
                val d = doc.data ?: return@mapNotNull null
                RetroEvent(
                    id = doc.id,
                    title = d["title"] as? String ?: "",
                    description = d["description"] as? String ?: "",
                    date = (d["date"] as? Long) ?: 0L,
                    type = d["type"] as? String ?: "COMMUNITY",
                    authorUid = d["authorUid"] as? String ?: "",
                    authorUsername = d["authorUsername"] as? String ?: "",
                    emoji = d["emoji"] as? String ?: "🎮"
                )
            }.filter { it.title.isNotBlank() }
        } catch (_: Exception) {}
    }

    // Platform + genre filtered lists
    val filteredIgdbGames = remember(igdbGames, selectedPlatform, selectedGenre) {
        igdbGames.filter { g ->
            (selectedPlatform == "ALL" || g.platforms.any { p -> platformAbbr(p) == selectedPlatform }) &&
            (selectedGenre == "ALL"    || g.genres.any { genre -> genreAbbr(genre) == selectedGenre })
        }
    }
    val filteredIgdbDLCs = remember(igdbDLCs, selectedPlatform, selectedGenre) {
        igdbDLCs.filter { g ->
            (selectedPlatform == "ALL" || g.platforms.any { p -> platformAbbr(p) == selectedPlatform }) &&
            (selectedGenre == "ALL"    || g.genres.any { genre -> genreAbbr(genre) == selectedGenre })
        }
    }

    // Available genres across all loaded games (for the filter row)
    val availableGenres = remember(igdbGames) {
        igdbGames.flatMap { it.genres }.map { genreAbbr(it) }.distinct().sorted().take(8)
    }

    // Convert IGDB items → RetroEvent for calendar
    val igdbAsEvents = remember(igdbGames) {
        igdbGames.map { g ->
            RetroEvent(id = "igdb_${g.id}", title = g.name,
                description = g.summary ?: "Upcoming release",
                date = g.releaseDate * 1000L, type = "UPCOMING", emoji = "🎮", coverUrl = g.coverUrl)
        }
    }
    val igdbDLCAsEvents = remember(igdbDLCs) {
        igdbDLCs.map { g ->
            RetroEvent(id = "igdb_dlc_${g.id}", title = g.name,
                description = g.summary ?: "New DLC / Expansion",
                date = g.releaseDate * 1000L, type = "DLC", emoji = "📦", coverUrl = g.coverUrl)
        }
    }

    val allEvents = remember(communityEvents, igdbAsEvents, igdbDLCAsEvents) {
        (igdbAsEvents + igdbDLCAsEvents + communityEvents)
            .sortedBy { event ->
                val cal = Calendar.getInstance(); cal.timeInMillis = event.date
                cal.get(Calendar.MONTH) * 100 + cal.get(Calendar.DAY_OF_MONTH)
            }
    }

    val filteredEvents = remember(allEvents, selectedFilter) {
        if (selectedFilter == "ALL") allEvents else allEvents.filter { it.type == selectedFilter }
    }

    val eventsForSelectedDay = remember(filteredEvents, selectedDay, displayedMonth, displayedYear) {
        filteredEvents.filter { event ->
            val cal = Calendar.getInstance(); cal.timeInMillis = event.date
            cal.get(Calendar.MONTH) == displayedMonth &&
                    cal.get(Calendar.DAY_OF_MONTH) == selectedDay &&
                    cal.get(Calendar.YEAR) == displayedYear
        }
    }

    val eventsForMonth = remember(filteredEvents, displayedMonth, displayedYear) {
        filteredEvents.filter { event ->
            val cal = Calendar.getInstance(); cal.timeInMillis = event.date
            cal.get(Calendar.MONTH) == displayedMonth && cal.get(Calendar.YEAR) == displayedYear
        }
    }

    val daysWithEventTypes = remember(allEvents, displayedMonth, displayedYear) {
        val map = mutableMapOf<Int, MutableSet<String>>()
        allEvents.forEach { event ->
            val cal = Calendar.getInstance(); cal.timeInMillis = event.date
            if (cal.get(Calendar.MONTH) == displayedMonth && cal.get(Calendar.YEAR) == displayedYear) {
                map.getOrPut(cal.get(Calendar.DAY_OF_MONTH)) { mutableSetOf() }.add(event.type)
            }
        }
        map as Map<Int, Set<String>>
    }

    val todayEvents = remember(allEvents) {
        allEvents.filter { event ->
            val cal = Calendar.getInstance(); cal.timeInMillis = event.date
            cal.get(Calendar.MONTH)       == today.get(Calendar.MONTH) &&
                    cal.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH) &&
                    cal.get(Calendar.YEAR)         == today.get(Calendar.YEAR)
        }
    }

    val featuredGame = remember(igdbGames) { igdbGames.maxByOrNull { it.hypes } }

    if (showAddEvent) {
        AddEventSheet(
            authorUid = currentUser?.uid ?: "",
            authorUsername = firebaseProfile?.username ?: "",
            onDismiss = { showAddEvent = false },
            onSaved = { event -> communityEvents = communityEvents + event; showAddEvent = false }
        )
        return
    }

    Box(modifier = modifier.fillMaxSize().background(ComicGlassBg)) {
        HalftoneBackground(modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(CGreen, CGreenMint, CGreen)))
                    .border(BorderStroke(2.dp, ScrapbookBorder))
                    .padding(top = 16.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("📅 EVENTS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 30.sp, letterSpacing = 2.sp)
                        Text("Upcoming games • releases • shows", fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold, color = ScrapbookDark.copy(alpha = 0.55f), fontSize = 11.sp)
                    }
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookDark)
                        .padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("${allEvents.size}", fontFamily = BangersFontFamily, color = CGreen, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (currentUser != null && firebaseProfile?.setupComplete == true) {
                        var addPressed by remember { mutableStateOf(false) }
                        val addScale by animateFloatAsState(targetValue = if (addPressed) 0.88f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "addScale")
                        Box(modifier = Modifier.scale(addScale).size(40.dp).clip(CircleShape)
                            .background(ScrapbookDark).clickable { addPressed = true; showAddEvent = true },
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Add, contentDescription = "Add", tint = CGreen, modifier = Modifier.size(20.dp))
                        }
                        LaunchedEffect(addPressed) { if (addPressed) { delay(150); addPressed = false } }
                    }
                }
            }

            // ── Filter chips ──────────────────────────────────────────────────
            LazyRow(
                modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.92f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("ALL", "UPCOMING", "DLC", "COMMUNITY")
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    val chipColor  = if (filter == "ALL") ScrapbookDark else eventTypeColor(filter)
                    var pressed by remember { mutableStateOf(false) }
                    val chipScale by animateFloatAsState(targetValue = if (pressed) 0.92f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "chip_$filter")
                    Box(modifier = Modifier.scale(chipScale)) {
                        if (isSelected) {
                            GlowPulse(
                                modifier = Modifier.matchParentSize(),
                                glowColor = chipColor,
                                cornerRadius = 20.dp,
                                maxAlpha = 0.45f
                            )
                        }
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) chipColor.copy(alpha = 0.12f) else ComicGlassBg)
                                .border(if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) chipColor else ScrapbookDark.copy(alpha = 0.18f),
                                    RoundedCornerShape(20.dp))
                                .clickable { pressed = true; selectedFilter = filter }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = when (filter) {
                                    "ALL"       -> "✦ ALL"
                                    "UPCOMING"  -> "🎮 UPCOMING"
                                    "DLC"       -> "📦 DLC"
                                    "COMMUNITY" -> "👥 COMMUNITY"
                                    else        -> filter
                                },
                                fontFamily = BangersFontFamily,
                                color = if (isSelected) ScrapbookDark else ScrapbookDark.copy(alpha = 0.55f),
                                fontSize = 12.sp
                            )
                        }
                    }
                    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {

                // ── This week at a glance (tap → full weekly update) ─────────
                item(key = "weekly_update") {
                    WeeklyUpdateCard(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp))
                }

                // ── Loading indicator ─────────────────────────────────────────
                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator(color = CGreen, strokeWidth = 3.dp)
                                Text("FETCHING UPCOMING GAMES...", fontFamily = BangersFontFamily,
                                    color = ScrapbookDark.copy(alpha = 0.45f), fontSize = 13.sp)
                            }
                        }
                    }
                }

                // ── Empty state (loaded but 0 results — likely API/credential issue) ──
                if (!isLoading && igdbGames.isEmpty() && igdbDLCs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.92f))
                                .border(1.dp, ScrapbookDark.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📡", fontSize = 36.sp)
                                Text("NO UPCOMING GAMES FOUND", fontFamily = BangersFontFamily,
                                    color = ScrapbookDark, fontSize = 16.sp, letterSpacing = 1.sp)
                                Text("Check Logcat for IGDB / token errors,\nor verify your Twitch API credentials.",
                                    fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.5f),
                                    fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                }

                // ── Featured hero (most hyped incoming game) ──────────────────
                if (featuredGame != null) {
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            EventsSectionHeader("🔥 MOST ANTICIPATED", CGreen, neonAlpha)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            FeaturedHeroCard(
                                game = featuredGame,
                                isWishlisted = wishlist.any { it == featuredGame.id },
                                onWishlist = {
                                    if (wishlist.any { it == featuredGame.id }) wishlist.removeAll { it == featuredGame.id }
                                    else wishlist.add(featuredGame.id)
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp),
                            color = ScrapbookDark.copy(alpha = 0.1f))
                    }
                }

                // ── Most Wanted horizontal cards ──────────────────────────────
                if (filteredIgdbGames.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            EventsSectionHeader("🎮 MOST WANTED", CGreen, neonAlpha, badge = "LIVE")
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Platform filter row
                        val platforms = listOf("ALL", "PS5", "XBX", "NSW", "PC")
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(platforms) { jumpIndex, plat ->
                                Box(modifier = Modifier.jumpIn(jumpIndex)) {
                                val isSel = selectedPlatform == plat
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) ScrapbookDark else Color.White.copy(alpha = 0.46f))
                                        .border(1.dp, ScrapbookDark.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                                        .clickable { selectedPlatform = plat }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = when (plat) { "NSW" -> "SWITCH"; else -> plat },
                                        fontFamily = BangersFontFamily,
                                        color = if (isSel) CGreen else ScrapbookDark.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }
                                                            }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        // Genre filter row (dynamically built from IGDB results)
                        if (availableGenres.isNotEmpty()) {
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                itemsIndexed(listOf("ALL") + availableGenres) { jumpIndex, genre ->
                                    Box(modifier = Modifier.jumpIn(jumpIndex)) {
                                    val isSel = selectedGenre == genre
                                    val genreColor = CGreen
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (isSel) genreColor.copy(alpha = 0.15f) else Color.Transparent)
                                            .border(if (isSel) 1.5.dp else 1.dp,
                                                if (isSel) genreColor else ScrapbookDark.copy(alpha = 0.15f),
                                                RoundedCornerShape(20.dp))
                                            .clickable { selectedGenre = genre }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (genre == "ALL") "ALL GENRES" else genre,
                                            fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
                                            color = if (isSel) ScrapbookDark else ScrapbookDark.copy(alpha = 0.5f),
                                            fontSize = 10.sp
                                        )
                                    }
                                                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(filteredIgdbGames.take(10), key = { "hw_${it.id}" }) { game ->
                                UpcomingReleaseCard(
                                    game = game,
                                    isWishlisted = wishlist.any { it == game.id },
                                    onWishlist = {
                                        if (wishlist.any { it == game.id }) wishlist.removeAll { it == game.id }
                                        else wishlist.add(game.id)
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp),
                            color = ScrapbookDark.copy(alpha = 0.1f))
                    }
                }

                // ── Today's highlight ─────────────────────────────────────────
                if (todayEvents.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            EventsSectionHeader("TODAY'S HIGHLIGHT", CGreen, neonAlpha)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            todayEvents.forEachIndexed { index, event ->
                                SpringEntrance(delayMs = index * 70) {
                                    EventCoverCard(event = event, neonAlpha = neonAlpha, height = 140.dp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // ── Calendar ──────────────────────────────────────────────────
                item {
                    if (todayEvents.isEmpty()) Spacer(modifier = Modifier.height(12.dp))
                    RetroCalendarEnhanced(
                        displayedMonth = displayedMonth, displayedYear = displayedYear,
                        selectedDay = selectedDay, daysWithEventTypes = daysWithEventTypes,
                        today = today, onDaySelected = { selectedDay = it },
                        onPrevMonth = { if (displayedMonth == 0) { displayedMonth = 11; displayedYear-- } else displayedMonth-- },
                        onNextMonth = { if (displayedMonth == 11) { displayedMonth = 0; displayedYear++ } else displayedMonth++ },
                        neonAlpha = neonAlpha
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── Selected day ──────────────────────────────────────────────
                if (eventsForSelectedDay.isNotEmpty()) {
                    item {
                        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).let {
                            val cal = Calendar.getInstance(); cal.set(Calendar.MONTH, displayedMonth); it.format(cal.time)
                        }
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            EventsSectionHeader("📍 $monthName $selectedDay", CGreen, neonAlpha,
                                badge = "${eventsForSelectedDay.size}")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    itemsIndexed(eventsForSelectedDay, key = { _, e -> "sel_${e.id}" }) { index, event ->
                        SpringEntrance(delayMs = index * 70) {
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                EventCoverCard(event = event, neonAlpha = neonAlpha)
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }

                // ── This month ────────────────────────────────────────────────
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        EventsSectionHeader("THIS MONTH", ScrapbookDark, neonAlpha, badge = "${eventsForMonth.size}")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (eventsForMonth.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📭", fontSize = 40.sp)
                                Text("NO EVENTS THIS MONTH", fontFamily = BangersFontFamily,
                                    color = ScrapbookDark.copy(alpha = 0.4f), fontSize = 16.sp)
                            }
                        }
                    }
                } else {
                    itemsIndexed(eventsForMonth, key = { _, e -> "month_${e.id}" }) { index, event ->
                        SpringEntrance(delayMs = index * 70) {
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                EventCoverCard(event = event, neonAlpha = neonAlpha)
                            }
                        }
                    }
                }

                // ── Upcoming Releases (live IGDB) ─────────────────────────────
                if (filteredIgdbGames.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            EventsSectionHeader("NEW & UPCOMING RELEASES", CGreen, neonAlpha,
                                badge = "${filteredIgdbGames.size} LIVE")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    itemsIndexed(filteredIgdbGames.sortedBy { it.releaseDate }, key = { _, g -> "igdbg_${g.id}" }) { index, game ->
                        SpringEntrance(delayMs = index * 70) {
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                IGDBGameCard(
                                    game = game, type = "UPCOMING",
                                    isWishlisted = wishlist.any { it == game.id },
                                    onWishlist = {
                                        if (wishlist.any { it == game.id }) wishlist.removeAll { it == game.id }
                                        else wishlist.add(game.id)
                                    },
                                    neonAlpha = neonAlpha
                                )
                            }
                        }
                    }
                }

                // ── DLC & Updates (live IGDB) ─────────────────────────────────
                if (filteredIgdbDLCs.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            EventsSectionHeader("DLC, EXPANSIONS & UPDATES", CAcBlue, neonAlpha, badge = "${filteredIgdbDLCs.size} LIVE")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    itemsIndexed(filteredIgdbDLCs.sortedBy { it.releaseDate }, key = { _, g -> "igdbdlc_${g.id}" }) { index, game ->
                        SpringEntrance(delayMs = index * 70) {
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                IGDBGameCard(
                                    game = game, type = "DLC",
                                    isWishlisted = wishlist.any { it == game.id },
                                    onWishlist = {
                                        if (wishlist.any { it == game.id }) wishlist.removeAll { it == game.id }
                                        else wishlist.add(game.id)
                                    },
                                    neonAlpha = neonAlpha
                                )
                            }
                        }
                    }
                }

                // ── Community ─────────────────────────────────────────────────
                if (communityEvents.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            EventsSectionHeader("COMMUNITY EVENTS", CAcBlue, neonAlpha)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    itemsIndexed(communityEvents, key = { _, e -> "com_${e.id}" }) { index, event ->
                        SpringEntrance(delayMs = index * 70) {
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                EventCoverCard(event = event, neonAlpha = neonAlpha)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Section Header ────────────────────────────────────────────────────────────

@Composable
fun EventsSectionHeader(label: String, color: Color, neonAlpha: Float, badge: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp))
            .background(color.copy(alpha = neonAlpha)))
        Text(label, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp,
            modifier = Modifier.weight(1f))
        if (badge != null) {
            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.12f))
                .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text(badge, fontFamily = BangersFontFamily, color = color, fontSize = 10.sp)
            }
        }
    }
}

// ─── Trailer WebView (shared by Featured + Game cards) ─────────────────────────

@Composable
fun TrailerWebView(videoId: String, onClose: () -> Unit) {
    val html = remember(videoId) { """
        <!DOCTYPE html><html>
        <head><meta name="viewport" content="width=device-width,initial-scale=1,user-scalable=no">
        <style>*{margin:0;padding:0;}html,body{width:100%;height:100%;background:#000;}iframe{width:100%;height:100%;border:none;}</style>
        </head>
        <body><iframe src="https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1&rel=0&modestbranding=1&controls=1"
          allow="autoplay;encrypted-media;picture-in-picture;fullscreen" allowfullscreen></iframe></body></html>
    """.trimIndent() }

    Box(
        modifier = Modifier.fillMaxWidth().height(210.dp)
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        // Close button
        Box(
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(32.dp).clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.65f)).clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Close trailer", tint = Color.White,
                modifier = Modifier.size(16.dp))
        }
    }
}

// ─── Featured Hero Card ────────────────────────────────────────────────────────

@Composable
fun FeaturedHeroCard(game: IGDBUpcomingGame, isWishlisted: Boolean, onWishlist: () -> Unit) {
    var showTrailer by remember { mutableStateOf(false) }
    val countdown   = remember(game.releaseDate) { countdownForEpochSec(game.releaseDate) }
    val dateStr     = remember(game.releaseDate) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(game.releaseDate * 1000L))
    }
    val hypeRatio   = (game.hypes.toFloat() / 500f).coerceIn(0f, 1f)
    val platformTxt = game.platforms.mapNotNull { platformAbbr(it) }.distinct().take(4).joinToString(" · ")
    val genreTxt    = game.genres.take(3).joinToString(" · ") { genreAbbr(it) }

    Column {
        Box(
            modifier = Modifier.fillMaxWidth().height(230.dp)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp,
                    bottomStart = if (showTrailer) 0.dp else 18.dp,
                    bottomEnd   = if (showTrailer) 0.dp else 18.dp))
                .shadow(8.dp, RoundedCornerShape(18.dp))
        ) {
            if (game.coverUrl != null) {
                AsyncImage(model = game.coverUrl, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.halftoneReveal(game.coverUrl).fillMaxSize())
            } else {
                Box(modifier = Modifier.fillMaxSize()
                    .background(Brush.linearGradient(listOf(CGreen.copy(alpha = 0.4f), ScrapbookDark))))
            }
            Box(modifier = Modifier.fillMaxSize()
                .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.85f), Color.Black.copy(alpha = 0.2f)))))
            Box(modifier = Modifier.fillMaxWidth().height(1.5.dp).align(Alignment.TopCenter)
                .background(Color.White.copy(alpha = 0.2f)))

            Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
                // Top row: badges + wishlist
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(CGreen)
                        .padding(horizontal = 10.dp, vertical = 5.dp)) {
                        Text("⭐ MOST HYPED", fontFamily = BangersFontFamily, color = ScrapbookDark,
                            fontSize = 9.sp, letterSpacing = 1.sp)
                    }
                    // Developer badge
                    if (game.developer != null) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp)) {
                            Text(game.developer, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.85f), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    // ESRB badge
                    if (game.esrbRating != null) {
                        Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp))
                            .background(esrbColor(game.esrbRating))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center) {
                            Text(game.esrbRating, fontFamily = BangersFontFamily, color = Color.White,
                                fontSize = 8.sp, textAlign = TextAlign.Center)
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    // Wishlist heart
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(if (isWishlisted) CGreen.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.15f))
                            .border(1.dp, if (isWishlisted) CGreen else Color.White.copy(alpha = 0.3f), CircleShape)
                            .clickable { onWishlist() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isWishlisted) "♥" else "♡",
                            color = if (isWishlisted) ScrapbookDark else Color.White, fontSize = 16.sp)
                    }
                }

                // Bottom info
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(game.name, fontFamily = BangersFontFamily, color = Color.White, fontSize = 26.sp,
                        lineHeight = 28.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (game.summary != null) {
                        Text(game.summary, fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                    }
                    // Genre + game mode row
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (genreTxt.isNotBlank()) {
                            Text(genreTxt, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                        }
                        if (game.gameModes.isNotEmpty()) {
                            Text("·", color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp)
                            game.gameModes.take(3).forEach { mode ->
                                Text(gameModeIcon(mode), fontSize = 12.sp)
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(CGreen)
                            .padding(horizontal = 10.dp, vertical = 5.dp)) {
                            Text(countdown, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp)
                        }
                        Text(dateStr, fontFamily = NunitoFontFamily, fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
                        if (platformTxt.isNotBlank()) {
                            Text(platformTxt, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (game.hypes > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)) {
                                Text("🔥", fontSize = 10.sp)
                                Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.15f))) {
                                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(hypeRatio)
                                        .background(Brush.horizontalGradient(listOf(CGreen, CGreenDeep))))
                                }
                                Text("${game.hypes}", fontFamily = BangersFontFamily, color = CGreen, fontSize = 9.sp)
                            }
                        }
                        // Trailer button (only if IGDB has a video ID)
                        if (game.trailerVideoId != null) {
                            RetroGlassButton(
                                text = if (showTrailer) "✕ CLOSE" else "▶ TRAILER",
                                onClick = { showTrailer = !showTrailer },
                                modifier = Modifier
                            )
                        }
                    }
                }
            }
        }

        // ── Live release countdown + Add to calendar ──
        Spacer(modifier = Modifier.height(10.dp))
        ReleaseCountdownPanel(title = game.name, releaseMillis = game.releaseDate * 1000L)

        // Animated trailer expansion
        AnimatedVisibility(
            visible = showTrailer && game.trailerVideoId != null,
            enter = expandVertically(animationSpec = tween(350, easing = EaseOutCubic)) + fadeIn(tween(250)),
            exit  = shrinkVertically(animationSpec = tween(300, easing = EaseInCubic)) + fadeOut(tween(200))
        ) {
            if (game.trailerVideoId != null) {
                TrailerWebView(videoId = game.trailerVideoId, onClose = { showTrailer = false })
            }
        }
    }
}

// ─── Upcoming Release Card (horizontal strip) ─────────────────────────────────

@Composable
fun UpcomingReleaseCard(game: IGDBUpcomingGame, isWishlisted: Boolean, onWishlist: () -> Unit) {
    val countdown = remember(game.releaseDate) { countdownForEpochSec(game.releaseDate) }
    val dateStr   = remember(game.releaseDate) {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(game.releaseDate * 1000L))
    }
    val cdColor = when (countdown) {
        "TODAY!" -> CAcRed; "TOMORROW" -> CGreenDeep
        "RELEASED" -> ScrapbookDark.copy(alpha = 0.4f); else -> CGreen
    }
    var pressed by remember { mutableStateOf(false) }
    val glowAlpha by rememberGlowPhase(0.4f)
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "upPress")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "upShadow")

    Box(modifier = Modifier.width(150.dp).height(200.dp)) {
        Box(modifier = Modifier.matchParentSize().offset(x = 6.dp, y = 6.dp).clip(RoundedCornerShape(14.dp)).background(CGreen.copy(alpha = glowAlpha * 0.28f)))
        Box(modifier = Modifier.matchParentSize().offset(x = shadowOff.dp, y = shadowOff.dp).clip(RoundedCornerShape(14.dp)).background(CGreen))
        Box(
            modifier = Modifier.width(150.dp).height(200.dp)
                .offset(x = pressAnim.dp, y = pressAnim.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))
                .clickable { pressed = true }
        ) {
        // Green stripe
        Box(modifier = Modifier.fillMaxWidth().height(4.dp)
            .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
        if (game.coverUrl != null) {
            // Green stripe
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
            AsyncImage(model = game.coverUrl, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.halftoneReveal(game.coverUrl).fillMaxSize())
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(
                listOf(CGreen.copy(alpha = 0.2f), Color.White.copy(alpha = 0.46f)))))
        }
        Box(modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f)))))
        // Specular
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter)
            .background(Color.White.copy(alpha = 0.35f)))
        // Wishlist
        Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(28.dp).clip(CircleShape)
            .background(if (isWishlisted) CGreen else Color.Black.copy(alpha = 0.45f))
            .clickable { onWishlist() }, contentAlignment = Alignment.Center) {
            Text(if (isWishlisted) "♥" else "♡",
                color = if (isWishlisted) ScrapbookDark else Color.White, fontSize = 12.sp)
        }
        Column(modifier = Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(CGreen.copy(alpha = 0.9f))
                .padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text("NEW", fontFamily = BangersFontFamily, color = Color.White, fontSize = 9.sp, letterSpacing = 1.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(game.name, fontFamily = BangersFontFamily, color = Color.White, fontSize = 13.sp,
                    lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(cdColor.copy(alpha = 0.9f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)) {
                    Text(countdown, fontFamily = BangersFontFamily, color = Color.White, fontSize = 9.sp)
                }
                Text(dateStr, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp)
            }
        }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── IGDB Game Card (list row, cream style, full data) ────────────────────────

@Composable
fun IGDBGameCard(
    game: IGDBUpcomingGame, type: String,
    isWishlisted: Boolean, onWishlist: () -> Unit, neonAlpha: Float
) {
    var showTrailer by remember { mutableStateOf(false) }
    val typeColor   = if (type == "DLC") CAcBlue else CGreen
    val countdown   = remember(game.releaseDate) { countdownForEpochSec(game.releaseDate) }
    val dateStr     = remember(game.releaseDate) {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(game.releaseDate * 1000L))
    }
    val platformTxt = game.platforms.mapNotNull { platformAbbr(it) }.distinct().take(3).joinToString(" · ")
    val hypeRatio   = (game.hypes.toFloat() / 300f).coerceIn(0f, 1f)
    val cdColor     = when (countdown) {
        "TODAY!"   -> CAcRed;  "TOMORROW" -> CGreenDeep
        "RELEASED" -> ScrapbookDark.copy(alpha = 0.35f); else -> typeColor
    }
    var pressed by remember { mutableStateOf(false) }
    val glowAlpha by rememberGlowPhase(0.4f)
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "igdbPress")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "igdbShadow")

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.matchParentSize().offset(x = 6.dp, y = 6.dp).clip(RoundedCornerShape(14.dp)).background(CGreen.copy(alpha = glowAlpha * 0.28f)))
            Box(modifier = Modifier.matchParentSize().offset(x = shadowOff.dp, y = shadowOff.dp).clip(RoundedCornerShape(14.dp)).background(CGreen))
        Box(
            modifier = Modifier.fillMaxWidth()
                .offset(x = pressAnim.dp, y = pressAnim.dp)
                .clip(RoundedCornerShape(
                    topStart = 14.dp, topEnd = 14.dp,
                    bottomStart = if (showTrailer) 0.dp else 14.dp,
                    bottomEnd   = if (showTrailer) 0.dp else 14.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))
                .clickable { pressed = true }
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter)
                .background(Color.White.copy(alpha = 0.7f)))

            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                // Cover thumbnail with ESRB overlay
                Box(modifier = Modifier.width(70.dp).fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = if (showTrailer) 0.dp else 14.dp))) {
                    if (game.coverUrl != null) {
                        AsyncImage(model = game.coverUrl, contentDescription = null,
                            contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(game.coverUrl).fillMaxSize())
                    } else {
                        Box(modifier = Modifier.fillMaxSize()
                            .background(Brush.verticalGradient(listOf(typeColor.copy(alpha = 0.25f), typeColor.copy(alpha = 0.08f)))),
                            contentAlignment = Alignment.Center) {
                            Text(game.name.first().uppercaseChar().toString(),
                                fontFamily = BangersFontFamily, color = typeColor, fontSize = 28.sp)
                        }
                    }
                    // NEW/DLC badge at bottom-left
                    Box(modifier = Modifier.align(Alignment.BottomStart).padding(5.dp)
                        .clip(RoundedCornerShape(4.dp)).background(typeColor.copy(alpha = 0.9f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)) {
                        Text(releaseTypeLabel(game.category, game.releaseDate), fontFamily = BangersFontFamily,
                            color = Color.White, fontSize = 8.sp)
                    }
                    // ESRB badge at top-right
                    if (game.esrbRating != null) {
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp)
                            .clip(RoundedCornerShape(3.dp)).background(esrbColor(game.esrbRating))
                            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(3.dp)),
                            contentAlignment = Alignment.Center) {
                            Text(game.esrbRating, fontFamily = BangersFontFamily, color = Color.White,
                                fontSize = 7.sp, textAlign = TextAlign.Center)
                        }
                    }
                    ScanlineOverlay(Modifier.matchParentSize(), lineAlpha = 0.06f)
                }

                // Content
                Column(
                    modifier = Modifier.weight(1f).padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Title + developer
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(game.name, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp,
                            lineHeight = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (game.developer != null) {
                            Text("by ${game.developer}", fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold, color = typeColor.copy(alpha = 0.7f),
                                fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        } else if (game.summary != null) {
                            Text(game.summary, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.55f),
                                fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    // Countdown + date + platforms
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        HoloBadge(label = countdown, cornerRadius = 4.dp, fontSize = 9.sp)
                        Text(dateStr, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
                            color = ScrapbookDark.copy(alpha = 0.45f), fontSize = 10.sp)
                        if (platformTxt.isNotBlank()) {
                            Text("·", color = ScrapbookDark.copy(alpha = 0.25f), fontSize = 10.sp)
                            Text(platformTxt, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
                                color = ScrapbookDark.copy(alpha = 0.35f), fontSize = 9.sp)
                        }
                    }

                    // Genre chips + game modes
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        game.genres.take(2).forEach { genre ->
                            HoloBadge(label = genreAbbr(genre), cornerRadius = 4.dp, fontSize = 7.sp)
                        }
                        if (game.gameModes.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(2.dp))
                            game.gameModes.take(3).forEach { mode ->
                                Text(gameModeIcon(mode), fontSize = 10.sp)
                            }
                        }
                    }

                    // Hype bar + trailer button row
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (game.hypes > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)) {
                                Text("🔥", fontSize = 9.sp)
                                Box(modifier = Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp))
                                    .background(ScrapbookDark.copy(alpha = 0.1f))) {
                                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(hypeRatio)
                                        .background(Brush.horizontalGradient(listOf(typeColor, typeColor.copy(alpha = 0.5f)))))
                                }
                                Text("${game.hypes}", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
                                    color = ScrapbookDark.copy(alpha = 0.35f), fontSize = 8.sp)
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        // Trailer button
                        if (game.trailerVideoId != null) {
                            RetroGlassButton(
                                text = if (showTrailer) "✕ CLOSE" else "▶ TRAILER",
                                onClick = { showTrailer = !showTrailer }
                            )
                        }
                    }
                }

                // Right: wishlist + date box
                Column(
                    modifier = Modifier.fillMaxHeight().padding(end = 12.dp, top = 10.dp, bottom = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier.size(30.dp).clip(CircleShape)
                            .background(if (isWishlisted) CGreen.copy(alpha = 0.15f) else Color.Transparent)
                            .border(1.dp, if (isWishlisted) CGreenDeep else ScrapbookDark.copy(alpha = 0.18f), CircleShape)
                            .clickable { onWishlist() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isWishlisted) "♥" else "♡",
                            color = if (isWishlisted) CGreenDeep else ScrapbookDark.copy(alpha = 0.3f), fontSize = 13.sp)
                    }
                    Column(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(typeColor.copy(alpha = 0.07f))
                            .border(1.dp, typeColor.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(dateStr.split(" ").getOrElse(0) { "" }.uppercase(),
                            fontFamily = BangersFontFamily, color = typeColor, fontSize = 9.sp, letterSpacing = 1.sp)
                        Text(dateStr.split(" ").getOrElse(1) { "" }, fontFamily = BangersFontFamily,
                            color = ScrapbookDark, fontSize = 17.sp, lineHeight = 19.sp)
                    }
                }
            }
        }

        }
        // Compact countdown + calendar shortcut
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelCountdown(targetMillis = game.releaseDate * 1000L, compact = true)
            Spacer(modifier = Modifier.weight(1f))
            if (game.releaseDate * 1000L > System.currentTimeMillis()) {
                val calContext = LocalContext.current
                ComicHeaderChip("📅 ADD") { addReleaseToCalendar(calContext, game.name, game.releaseDate * 1000L) }
            }
        }

        // Animated trailer expansion
        AnimatedVisibility(
            visible = showTrailer && game.trailerVideoId != null,
            enter = expandVertically(animationSpec = tween(350, easing = EaseOutCubic)) + fadeIn(tween(250)),
            exit  = shrinkVertically(animationSpec = tween(300, easing = EaseInCubic)) + fadeOut(tween(200))
        ) {
            if (game.trailerVideoId != null) {
                TrailerWebView(videoId = game.trailerVideoId, onClose = { showTrailer = false })
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Event Cover Card (conventions / esports / anniversaries / community) ─────

@Composable
fun EventCoverCard(event: RetroEvent, neonAlpha: Float, height: Dp = 110.dp) {
    val fetchedCover = rememberEventCover(event)
    val coverUrl = event.coverUrl ?: fetchedCover
    val typeColor = eventTypeColor(event.type)
    val dateStr   = remember(event.date) { SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(event.date)) }
    val countdown = remember(event.date) { countdownText(event.date) }
    var pressed by remember { mutableStateOf(false) }
    val glowAlpha by rememberGlowPhase(0.4f)
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "coverPress")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "coverShadow")

    Box(modifier = Modifier.fillMaxWidth().height(height)) {
        Box(modifier = Modifier.matchParentSize().offset(x = 6.dp, y = 6.dp).clip(RoundedCornerShape(14.dp)).background(CGreen.copy(alpha = glowAlpha * 0.28f)))
        Box(modifier = Modifier.matchParentSize().offset(x = shadowOff.dp, y = shadowOff.dp).clip(RoundedCornerShape(14.dp)).background(CGreen))
        Box(
            modifier = Modifier.fillMaxWidth().height(height)
                .offset(x = pressAnim.dp, y = pressAnim.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))
                .clickable { pressed = true }
        ) {
        // Specular
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter)
            .background(Color.White.copy(alpha = 0.7f)))

        Row(modifier = Modifier.fillMaxSize()) {
            // Left: cover art or emoji
            if (coverUrl != null) {
                Box(modifier = Modifier.width(72.dp).fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))) {
                    AsyncImage(model = coverUrl, contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(coverUrl).fillMaxSize())
                    Box(modifier = Modifier.fillMaxSize()
                        .background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.46f).copy(alpha = 0.55f)))))
                    ScanlineOverlay(Modifier.matchParentSize(), lineAlpha = 0.06f)
                }
            } else {
                Box(
                    modifier = Modifier.width(60.dp).fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                        .background(typeColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(event.emoji, fontSize = 26.sp)
                }
            }

            // Content
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    HoloBadge(label = eventTypeLabel(event.type), cornerRadius = 4.dp, fontSize = 8.sp)
                    Text(event.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp,
                        lineHeight = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(event.description, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.6f),
                        fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
                }
                if (event.authorUsername.isNotBlank()) {
                    Text("by ${event.authorUsername}", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
                        color = ScrapbookDark.copy(alpha = 0.32f), fontSize = 10.sp)
                }
            }

            // Right: date + countdown
            Column(
                modifier = Modifier.fillMaxHeight().padding(end = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        .background(typeColor.copy(alpha = 0.08f))
                        .border(1.dp, typeColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(dateStr.split(" ").getOrElse(0) { "" }.uppercase(),
                        fontFamily = BangersFontFamily, color = typeColor, fontSize = 10.sp, letterSpacing = 1.sp)
                    Text(dateStr.split(" ").getOrElse(1) { "" },
                        fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, lineHeight = 24.sp)
                }
                if (countdown != "RELEASED") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                        .background(typeColor.copy(alpha = 0.08f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)) {
                        Text(countdown, fontFamily = BangersFontFamily, color = typeColor, fontSize = 8.sp)
                    }
                }
            }
        }
        ComicShimmer(Modifier.matchParentSize(), cornerRadius = 14.dp)
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Enhanced Calendar ─────────────────────────────────────────────────────────

@Composable
fun RetroCalendarEnhanced(
    displayedMonth: Int, displayedYear: Int, selectedDay: Int,
    daysWithEventTypes: Map<Int, Set<String>>, today: Calendar,
    onDaySelected: (Int) -> Unit, onPrevMonth: () -> Unit, onNextMonth: () -> Unit,
    neonAlpha: Float
) {
    val monthName = remember(displayedMonth, displayedYear) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, displayedMonth); cal.set(Calendar.YEAR, displayedYear)
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }
    val daysInMonth = remember(displayedMonth, displayedYear) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, displayedMonth); cal.set(Calendar.YEAR, displayedYear)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    val firstDayOfWeek = remember(displayedMonth, displayedYear) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, displayedMonth); cal.set(Calendar.YEAR, displayedYear)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        (cal.get(Calendar.DAY_OF_WEEK) - 2).let { if (it < 0) 6 else it }
    }

    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .shadow(6.dp, RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .border(1.dp, ScrapbookDark.copy(alpha = 0.09f), RoundedCornerShape(18.dp))
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter)
                .background(Color.White.copy(alpha = 0.8f)))

            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    var prevPressed by remember { mutableStateOf(false) }
                    val prevScale by animateFloatAsState(targetValue = if (prevPressed) 0.88f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "prevScale")
                    Box(modifier = Modifier.scale(prevScale).size(36.dp).clip(CircleShape)
                        .background(ComicGlassBg)
                        .border(1.dp, ScrapbookDark.copy(alpha = 0.15f), CircleShape)
                        .clickable { prevPressed = true; onPrevMonth() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(16.dp))
                    }
                    LaunchedEffect(prevPressed) { if (prevPressed) { delay(150); prevPressed = false } }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(monthName.uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark,
                            fontSize = 17.sp, letterSpacing = 1.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            listOf("ANN" to CGreenDeep, "NEW" to CGreen, "DLC" to CAcBlue,
                                "CON" to CAcRed, "ESP" to CAcPurple).forEach { tagPair ->
                                val label = tagPair.first; val color = tagPair.second
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(color))
                                    Text(label, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
                                        color = ScrapbookDark.copy(alpha = 0.45f), fontSize = 7.sp)
                                }
                            }
                        }
                    }

                    var nextPressed by remember { mutableStateOf(false) }
                    val nextScale by animateFloatAsState(targetValue = if (nextPressed) 0.88f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "nextScale")
                    Box(modifier = Modifier.scale(nextScale).size(36.dp).clip(CircleShape)
                        .background(ComicGlassBg)
                        .border(1.dp, ScrapbookDark.copy(alpha = 0.15f), CircleShape)
                        .clickable { nextPressed = true; onNextMonth() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(16.dp))
                    }
                    LaunchedEffect(nextPressed) { if (nextPressed) { delay(150); nextPressed = false } }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = ScrapbookDark.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { day ->
                        Text(day, fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.38f),
                            fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                val totalCells = firstDayOfWeek + daysInMonth
                val rows = (totalCells + 6) / 7
                for (row in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val day = cellIndex - firstDayOfWeek + 1
                            if (day < 1 || day > daysInMonth) {
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val isToday   = day == today.get(Calendar.DAY_OF_MONTH) &&
                                        displayedMonth == today.get(Calendar.MONTH) &&
                                        displayedYear  == today.get(Calendar.YEAR)
                                val isSelected   = day == selectedDay
                                val eventTypes   = daysWithEventTypes[day] ?: emptySet()
                                var dayPressed by remember { mutableStateOf(false) }
                                val dayScale by animateFloatAsState(targetValue = if (dayPressed) 0.85f else 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "day_$day")

                                Box(
                                    modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp).scale(dayScale)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(when {
                                            isSelected              -> ScrapbookDark
                                            isToday                 -> CGreen.copy(alpha = 0.3f)
                                            eventTypes.isNotEmpty() -> ComicGlassBg
                                            else                    -> Color.Transparent
                                        })
                                        .then(if (isToday && !isSelected)
                                            Modifier.border(1.5.dp, CGreen, RoundedCornerShape(8.dp))
                                        else Modifier)
                                        .clickable { dayPressed = true; onDaySelected(day) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize().padding(2.dp)) {
                                        Text(
                                            text = "$day",
                                            fontFamily = if (isSelected || isToday) BangersFontFamily else NunitoFontFamily,
                                            color = when {
                                                isSelected              -> CGreen
                                                isToday                 -> ScrapbookDark
                                                eventTypes.isNotEmpty() -> ScrapbookDark
                                                else                    -> ScrapbookDark.copy(alpha = 0.32f)
                                            },
                                            fontSize = 13.sp,
                                            fontWeight = if (eventTypes.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (eventTypes.isNotEmpty()) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                modifier = Modifier.padding(top = 1.dp)) {
                                                eventTypes.take(3).forEach { type ->
                                                    Box(modifier = Modifier.size(4.dp).clip(CircleShape)
                                                        .background(if (isSelected) CGreen.copy(alpha = 0.7f)
                                                        else eventTypeColor(type)))
                                                }
                                            }
                                        }
                                    }
                                }
                                LaunchedEffect(dayPressed) { if (dayPressed) { delay(150); dayPressed = false } }
                            }
                        }
                    }
                    if (row < rows - 1) Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}

// ─── Add Event Sheet ───────────────────────────────────────────────────────────

@Composable
fun AddEventSheet(
    authorUid: String, authorUsername: String,
    onDismiss: () -> Unit, onSaved: (RetroEvent) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🎮") }
    var isSaving by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }
    var selectedDay by remember { mutableStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }
    val emojiOptions = listOf("🎮", "🕹️", "👾", "🏆", "🎯", "💾", "📺", "🎲", "⭐", "🔥", "💿", "🧩")
    val neonAlpha by rememberGlowRange(0.4f, 1f)
    val isValid = title.isNotBlank()

    Box(modifier = Modifier.fillMaxSize().background(ComicGlassBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(CGreen, CGreenMint, CGreen)))
                    .border(BorderStroke(2.dp, ScrapbookBorder))
                    .padding(top = 16.dp, bottom = 12.dp, start = 4.dp, end = 16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = ScrapbookDark)
                    }
                    Text("ADD EVENT", fontFamily = BangersFontFamily, color = ScrapbookDark,
                        fontSize = 26.sp, modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                            .background(if (isValid) ScrapbookDark else ScrapbookDark.copy(alpha = 0.3f))
                            .clickable(enabled = isValid && !isSaving) {
                                isSaving = true
                                val date = dateOf(selectedMonth, selectedDay)
                                val event = RetroEvent(
                                    id = System.currentTimeMillis().toString(), title = title,
                                    description = description, date = date, type = "COMMUNITY",
                                    authorUid = authorUid, authorUsername = authorUsername, emoji = selectedEmoji
                                )
                                FirebaseFirestore.getInstance().collection("events").document(event.id)
                                    .set(mapOf("title" to event.title, "description" to event.description,
                                        "date" to event.date, "type" to event.type, "authorUid" to event.authorUid,
                                        "authorUsername" to event.authorUsername, "emoji" to event.emoji))
                                    .addOnSuccessListener { onSaved(event) }
                                    .addOnFailureListener { isSaving = false }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        if (isSaving) CircularProgressIndicator(color = CGreen,
                            modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text("SAVE", fontFamily = BangersFontFamily,
                            color = if (isValid) CGreen else CGreen.copy(alpha = 0.3f), fontSize = 16.sp)
                    }
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // Emoji picker
                item {
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .shadow(4.dp, RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.92f))
                        .border(1.dp, ScrapbookDark.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(14.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.width(4.dp).height(18.dp).clip(RoundedCornerShape(2.dp))
                                    .background(CGreen.copy(alpha = neonAlpha)))
                                Text("PICK AN EMOJI", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                emojiOptions.forEach { emoji ->
                                    val isSel = selectedEmoji == emoji
                                    var ep by remember { mutableStateOf(false) }
                                    val es by animateFloatAsState(
                                        targetValue = if (ep) 0.85f else if (isSel) 1.15f else 1f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "e_$emoji")
                                    Box(modifier = Modifier.scale(es).size(36.dp).clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) CGreen else ComicGlassBg)
                                        .border(if (isSel) 2.dp else 1.dp,
                                            if (isSel) ScrapbookDark else ScrapbookDark.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp))
                                        .clickable { ep = true; selectedEmoji = emoji },
                                        contentAlignment = Alignment.Center) {
                                        Text(emoji, fontSize = 17.sp)
                                    }
                                    LaunchedEffect(ep) { if (ep) { delay(150); ep = false } }
                                }
                            }
                        }
                    }
                }

                // Title
                item {
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .shadow(4.dp, RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.92f))
                        .border(1.dp, ScrapbookDark.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(14.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.width(4.dp).height(18.dp).clip(RoundedCornerShape(2.dp))
                                    .background(CGreen.copy(alpha = neonAlpha)))
                                Text("EVENT TITLE *", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                            }
                            OutlinedTextField(
                                value = title, onValueChange = { title = it },
                                placeholder = { Text("e.g. Mario Kart Night", fontFamily = NunitoFontFamily,
                                    fontSize = 13.sp, color = ScrapbookDark.copy(alpha = 0.35f)) },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookDark),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CGreen,
                                    unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.2f),
                                    focusedContainerColor = ComicGlassBg,
                                    unfocusedContainerColor = ComicGlassBg,
                                    cursorColor = CGreen,
                                    focusedTextColor = ScrapbookDark,
                                    unfocusedTextColor = ScrapbookDark
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Description
                item {
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .shadow(4.dp, RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.92f))
                        .border(1.dp, ScrapbookDark.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(14.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.width(4.dp).height(18.dp).clip(RoundedCornerShape(2.dp))
                                    .background(CGreen.copy(alpha = neonAlpha)))
                                Text("DESCRIPTION", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                            }
                            OutlinedTextField(
                                value = description, onValueChange = { description = it },
                                placeholder = { Text("What's this event about?", fontFamily = NunitoFontFamily,
                                    fontSize = 13.sp, color = ScrapbookDark.copy(alpha = 0.35f)) },
                                minLines = 3,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookDark),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CGreen,
                                    unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.2f),
                                    focusedContainerColor = ComicGlassBg,
                                    unfocusedContainerColor = ComicGlassBg,
                                    cursorColor = CGreen,
                                    focusedTextColor = ScrapbookDark,
                                    unfocusedTextColor = ScrapbookDark
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Date picker
                item {
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .shadow(4.dp, RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.92f))
                        .border(1.dp, ScrapbookDark.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(14.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.width(4.dp).height(18.dp).clip(RoundedCornerShape(2.dp))
                                    .background(CGreen.copy(alpha = neonAlpha)))
                                Text("SELECT DATE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("MONTH", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
                                        color = ScrapbookDark.copy(alpha = 0.5f), fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = "$selectedMonth",
                                        onValueChange = { selectedMonth = it.toIntOrNull()?.coerceIn(1, 12) ?: selectedMonth },
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookDark),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CGreen,
                                            unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.2f),
                                            focusedContainerColor = ComicGlassBg,
                                            unfocusedContainerColor = ComicGlassBg,
                                            cursorColor = CGreen,
                                            focusedTextColor = ScrapbookDark,
                                            unfocusedTextColor = ScrapbookDark
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("DAY", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
                                        color = ScrapbookDark.copy(alpha = 0.5f), fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = "$selectedDay",
                                        onValueChange = { selectedDay = it.toIntOrNull()?.coerceIn(1, 31) ?: selectedDay },
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookDark),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CGreen,
                                            unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.2f),
                                            focusedContainerColor = ComicGlassBg,
                                            unfocusedContainerColor = ComicGlassBg,
                                            cursorColor = CGreen,
                                            focusedTextColor = ScrapbookDark,
                                            unfocusedTextColor = ScrapbookDark
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            val previewDate = try { dateOf(selectedMonth, selectedDay) } catch (_: Exception) { 0L }
                            if (previewDate > 0L) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                        .background(CGreen.copy(alpha = 0.15f))
                                        .border(1.dp, CGreenDeep.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("📅", fontSize = 16.sp)
                                    Text(
                                        "Event on: ${SimpleDateFormat("MMMM d", Locale.getDefault()).format(Date(previewDate))}",
                                        fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
                                        color = ScrapbookDark, fontSize = 13.sp
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


// ─── Pixel LCD countdown + calendar ───────────────────────────────────────────

@Composable
fun PixelCountdown(targetMillis: Long, compact: Boolean = false) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(targetMillis) {
        while (true) { now = System.currentTimeMillis(); delay(1000) }
    }
    val remaining = targetMillis - now
    if (remaining <= 0L) {
        ArcadeBlinkText("OUT NOW!", androidx.compose.ui.text.TextStyle(fontFamily = BangersFontFamily,
            fontSize = if (compact) 14.sp else 26.sp, letterSpacing = 2.sp, color = CGreenDeep))
        return
    }
    val totalSec = remaining / 1000L
    val days = totalSec / 86400
    val hours = (totalSec % 86400) / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    val units = if (compact) listOf(days to "D", hours to "H")
                else listOf(days to "DAYS", hours to "HRS", minutes to "MIN", seconds to "SEC")
    Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp), verticalAlignment = Alignment.CenterVertically) {
        units.forEachIndexed { i, (value, label) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ScrapbookDark)
                        .border(2.dp, CGreen, RoundedCornerShape(6.dp))
                        .padding(horizontal = if (compact) 5.dp else 9.dp, vertical = if (compact) 2.dp else 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        RollingCounterText(
                            value.toString().padStart(2, '0'),
                            androidx.compose.ui.text.TextStyle(fontFamily = BangersFontFamily,
                                fontSize = if (compact) 12.sp else 24.sp, color = CGreenMint, letterSpacing = 1.sp)
                        )
                        if (compact) Text(label, fontFamily = BangersFontFamily, fontSize = 9.sp, color = CGreenMint)
                    }
                }
                if (!compact) Text(label, fontFamily = BangersFontFamily, fontSize = 9.sp, color = ScrapbookTextMuted)
            }
            if (!compact && i < units.lastIndex) {
                Text(":", fontFamily = BangersFontFamily, fontSize = 22.sp, color = ScrapbookDark,
                    modifier = Modifier.padding(bottom = 12.dp))
            }
        }
    }
}

fun addReleaseToCalendar(context: android.content.Context, title: String, millis: Long) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_INSERT)
            .setData(android.provider.CalendarContract.Events.CONTENT_URI)
            .putExtra(android.provider.CalendarContract.Events.TITLE, "🎮 $title release")
            .putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, millis)
            .putExtra(android.provider.CalendarContract.EXTRA_EVENT_ALL_DAY, true)
        context.startActivity(intent)
    } catch (_: Exception) { }
}

@Composable
fun ReleaseCountdownPanel(title: String, releaseMillis: Long) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp)
            .clip(RoundedCornerShape(14.dp)).background(CGreen))
        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.95f))
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("⏱ RELEASE COUNTDOWN", fontFamily = BangersFontFamily, fontSize = 16.sp,
                letterSpacing = 1.sp, color = ScrapbookDark)
            PixelCountdown(targetMillis = releaseMillis)
            if (releaseMillis > System.currentTimeMillis()) {
                RetroGlassButton(
                    text = "📅 ADD TO CALENDAR",
                    onClick = { addReleaseToCalendar(context, title, releaseMillis) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

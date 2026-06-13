package com.example.hubretro

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// ─── Background refresh worker (runs every 14 days) ───────────────────────────

class EventsRefreshWorker(context: android.content.Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val queries = listOf("upcoming 2025", "upcoming 2026", "new release 2025", "most anticipated 2026")
            val fetched = mutableListOf<Map<String, Any>>()
            queries.forEach { q ->
                try {
                    val results = IGDBRepository.searchGames(q)
                    results.filter { it.releaseYear != null && it.releaseYear >= 2025 }.take(4).forEach { game ->
                        if (fetched.none { (it["title"] as? String).equals(game.name, ignoreCase = true) }) {
                            fetched.add(mapOf(
                                "title" to game.name,
                                "description" to (game.summary?.take(120) ?: "Upcoming release"),
                                "date" to System.currentTimeMillis(),
                                "type" to "UPCOMING",
                                "emoji" to "🎮",
                                "coverUrl" to (game.coverUrl ?: ""),
                                "authorUid" to "",
                                "authorUsername" to "RetroHub"
                            ))
                        }
                    }
                } catch (e: Exception) { }
            }
            val db = FirebaseFirestore.getInstance()
            fetched.forEach { event ->
                val title = event["title"] as? String ?: return@forEach
                val safeId = "auto_${title.lowercase().replace(" ", "_").take(30)}"
                db.collection("auto_events").document(safeId).set(event)
            }
            Result.success()
        } catch (e: Exception) { Result.retry() }
    }
}

fun scheduleEventsRefresh(context: android.content.Context) {
    val request = PeriodicWorkRequestBuilder<EventsRefreshWorker>(14, TimeUnit.DAYS)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "events_refresh",
        ExistingPeriodicWorkPolicy.KEEP,
        request
    )
}

// ─── Event data ───────────────────────────────────────────────────────────────

val upcomingGameReleases = listOf(
    RetroEvent(id = "ugr1",  title = "Ghost of Yōtei",                 description = "Sucker Punch's follow-up set in 1603 Hokkaido.",               date = dateOf(10, 2),  type = "UPCOMING", emoji = "⛩️"),
    RetroEvent(id = "ugr2",  title = "Doom: The Dark Ages",             description = "id Software's brutal medieval Doom prequel.",                  date = dateOf(5, 15),  type = "UPCOMING", emoji = "⚔️"),
    RetroEvent(id = "ugr3",  title = "Grand Theft Auto VI",             description = "Rockstar's long-awaited return to Vice City.",                  date = dateOf(5, 26),  type = "UPCOMING", emoji = "🌴"),
    RetroEvent(id = "ugr4",  title = "Borderlands 4",                   description = "Gearbox's next entry in the looter-shooter franchise.",         date = dateOf(9, 12),  type = "UPCOMING", emoji = "🔫"),
    RetroEvent(id = "ugr5",  title = "Mafia: The Old Country",          description = "2K's prequel set in 1900s Sicily.",                            date = dateOf(8, 8),   type = "UPCOMING", emoji = "🇮🇹"),
    RetroEvent(id = "ugr6",  title = "Elden Ring: Nightreign",          description = "FromSoftware's standalone co-op spin-off.",                    date = dateOf(5, 30),  type = "UPCOMING", emoji = "🌑"),
    RetroEvent(id = "ugr7",  title = "Death Stranding 2: On the Beach", description = "Hideo Kojima's sequel to the strand game.",                    date = dateOf(6, 26),  type = "UPCOMING", emoji = "🌊"),
    RetroEvent(id = "ugr8",  title = "Metroid Prime 4: Beyond",         description = "Nintendo's long-awaited return to first-person Metroid.",      date = dateOf(7, 18),  type = "UPCOMING", emoji = "🚀"),
    RetroEvent(id = "ugr9",  title = "South of Midnight",               description = "Compulsion Games' dark Southern folklore action-adventure.",   date = dateOf(4, 8),   type = "UPCOMING", emoji = "🌿"),
    RetroEvent(id = "ugr10", title = "Atomfall",                        description = "Rebellion's British post-nuclear open world RPG.",             date = dateOf(3, 27),  type = "UPCOMING", emoji = "☢️"),
    RetroEvent(id = "ugr11", title = "The Outer Worlds 2",              description = "Obsidian's sequel to their beloved space RPG.",               date = dateOf(9, 19),  type = "UPCOMING", emoji = "🪐"),
    RetroEvent(id = "ugr12", title = "Fable",                           description = "Playground Games reimagines the beloved British RPG.",        date = dateOf(10, 16), type = "UPCOMING", emoji = "🧚")
)

val upcomingDLCUpdates = listOf(
    RetroEvent(id = "dlc1", title = "Monster Hunter Wilds",   description = "Title update 1 brings the fearsome Xu Wu to the hunt.",    date = dateOf(4, 3),  type = "DLC", emoji = "🐉"),
    RetroEvent(id = "dlc2", title = "Cyberpunk 2077",         description = "CD Projekt Red drops another major free update.",          date = dateOf(5, 20), type = "DLC", emoji = "🤖"),
    RetroEvent(id = "dlc3", title = "Elden Ring: Nightreign", description = "First major patch with balance changes and new content.", date = dateOf(6, 15), type = "DLC", emoji = "🗡️"),
    RetroEvent(id = "dlc4", title = "Fortnite",               description = "Chapter 6 Season 2 with new map changes and skins.",     date = dateOf(3, 14), type = "DLC", emoji = "🏝️"),
    RetroEvent(id = "dlc5", title = "Path of Exile 2",        description = "Early Access update adds new acts and endgame content.", date = dateOf(4, 11), type = "DLC", emoji = "💀"),
    RetroEvent(id = "dlc6", title = "No Man's Sky",           description = "Hello Games continues their incredible update streak.", date = dateOf(7, 4),  type = "DLC", emoji = "🌊"),
    RetroEvent(id = "dlc7", title = "Baldur's Gate 3",        description = "Larian's Patch 8 brings new subclasses.",               date = dateOf(3, 18), type = "DLC", emoji = "🧙"),
    RetroEvent(id = "dlc8", title = "Stardew Valley",         description = "ConcernedApe's next massive free content update.",      date = dateOf(8, 22), type = "DLC", emoji = "🌾")
)

val gamingConventions = listOf(
    RetroEvent(id = "con1",  title = "Summer Game Fest 2025",     description = "Geoff Keighley's annual showcase with world premieres.",            date = dateOf(6, 6),   type = "CONVENTION", emoji = "🎪"),
    RetroEvent(id = "con2",  title = "Gamescom 2025",             description = "Europe's largest gaming convention in Cologne, Germany.",          date = dateOf(8, 20),  type = "CONVENTION", emoji = "🇩🇪"),
    RetroEvent(id = "con3",  title = "Tokyo Game Show 2025",      description = "Japan's premier gaming expo showcasing Japanese developers.",      date = dateOf(9, 25),  type = "CONVENTION", emoji = "🇯🇵"),
    RetroEvent(id = "con4",  title = "PAX East 2025",             description = "The East Coast gaming convention in Boston.",                      date = dateOf(3, 6),   type = "CONVENTION", emoji = "🎮"),
    RetroEvent(id = "con5",  title = "PAX West 2025",             description = "The original PAX returns to Seattle.",                             date = dateOf(8, 29),  type = "CONVENTION", emoji = "🌲"),
    RetroEvent(id = "con6",  title = "Nintendo Direct — June",    description = "Nintendo's biannual showcase with major announcements.",           date = dateOf(6, 12),  type = "CONVENTION", emoji = "🍄"),
    RetroEvent(id = "con7",  title = "PlayStation State of Play", description = "Sony's showcase featuring PS exclusives and third-party titles.", date = dateOf(5, 8),   type = "CONVENTION", emoji = "🎯"),
    RetroEvent(id = "con8",  title = "Xbox Games Showcase",       description = "Microsoft's annual showcase with Game Pass titles.",              date = dateOf(6, 8),   type = "CONVENTION", emoji = "💚"),
    RetroEvent(id = "con9",  title = "The Game Awards 2025",      description = "Gaming's biggest night — world premieres and GOTY.",              date = dateOf(12, 11), type = "CONVENTION", emoji = "🏆"),
    RetroEvent(id = "con10", title = "EVO 2025",                  description = "The Evolution Championship Series for fighting games.",           date = dateOf(8, 1),   type = "CONVENTION", emoji = "🥊")
)

val esportsEvents = listOf(
    RetroEvent(id = "esp1", title = "LoL World Championship",      description = "The pinnacle of professional League of Legends.",                date = dateOf(10, 25), type = "ESPORTS", emoji = "⚡"),
    RetroEvent(id = "esp2", title = "The International — Dota 2", description = "Valve's annual Dota 2 world championship.",                      date = dateOf(9, 5),   type = "ESPORTS", emoji = "🌐"),
    RetroEvent(id = "esp3", title = "CS2 Major Championship",     description = "The biggest Counter-Strike 2 tournament of the year.",           date = dateOf(3, 15),  type = "ESPORTS", emoji = "💥"),
    RetroEvent(id = "esp4", title = "Valorant Champions 2025",    description = "Riot's flagship Valorant world championship event.",             date = dateOf(8, 2),   type = "ESPORTS", emoji = "🎯"),
    RetroEvent(id = "esp5", title = "Overwatch World Cup",        description = "Nations compete in Blizzard's team-based hero shooter.",        date = dateOf(11, 1),  type = "ESPORTS", emoji = "🦸"),
    RetroEvent(id = "esp6", title = "Rocket League World Champ.", description = "Psyonix's rocket-powered car soccer world finals.",              date = dateOf(6, 20),  type = "ESPORTS", emoji = "🚗"),
    RetroEvent(id = "esp7", title = "Street Fighter 6 Capcom Cup",description = "The premier Street Fighter competitive tournament.",             date = dateOf(2, 14),  type = "ESPORTS", emoji = "🥋"),
    RetroEvent(id = "esp8", title = "Fortnite World Cup Quals.",  description = "Epic's global Fortnite competitive season kicks off.",           date = dateOf(4, 19),  type = "ESPORTS", emoji = "🏆")
)

val retroAnniversaries = listOf(
    RetroEvent(id = "ann1",  title = "Super Mario Bros Launch",   description = "Super Mario Bros released for the NES in Japan.",                date = dateOf(9, 13),  type = "ANNIVERSARY", emoji = "🍄"),
    RetroEvent(id = "ann2",  title = "Game Boy Birthday",         description = "Nintendo's Game Boy released in Japan on April 21, 1989.",       date = dateOf(4, 21),  type = "ANNIVERSARY", emoji = "🎮"),
    RetroEvent(id = "ann3",  title = "Sonic the Hedgehog Launch", description = "Sonic debuted on the Sega Genesis on June 23, 1991.",            date = dateOf(6, 23),  type = "ANNIVERSARY", emoji = "💨"),
    RetroEvent(id = "ann4",  title = "PlayStation 1 Launch",      description = "Sony launched the original PlayStation in Japan, Dec 3, 1994.", date = dateOf(12, 3),  type = "ANNIVERSARY", emoji = "🎯"),
    RetroEvent(id = "ann5",  title = "Zelda: Ocarina of Time",    description = "Ocarina of Time released on November 21, 1998.",                date = dateOf(11, 21), type = "ANNIVERSARY", emoji = "🗡️"),
    RetroEvent(id = "ann6",  title = "Pac-Man Arcade Debut",      description = "Pac-Man appeared in arcades in Japan on May 22, 1980.",         date = dateOf(5, 22),  type = "ANNIVERSARY", emoji = "👾"),
    RetroEvent(id = "ann7",  title = "Tetris Day",                description = "Tetris released June 6, 1984 by Alexey Pajitnov.",              date = dateOf(6, 6),   type = "ANNIVERSARY", emoji = "🧩"),
    RetroEvent(id = "ann8",  title = "Doom Release Day",          description = "id Software released Doom as shareware on December 10, 1993.", date = dateOf(12, 10), type = "ANNIVERSARY", emoji = "🔫"),
    RetroEvent(id = "ann9",  title = "Nintendo NES Launch",       description = "The NES launched in North America on October 18, 1985.",        date = dateOf(10, 18), type = "ANNIVERSARY", emoji = "🕹️"),
    RetroEvent(id = "ann10", title = "Street Fighter II Launch",  description = "Street Fighter II hit arcades on February 6, 1991.",            date = dateOf(2, 6),   type = "ANNIVERSARY", emoji = "🥊"),
    RetroEvent(id = "ann11", title = "Pokemon Red & Blue Launch", description = "Pokémon Red and Blue launched in the US on September 28, 1998.",date = dateOf(9, 28),  type = "ANNIVERSARY", emoji = "⚡"),
    RetroEvent(id = "ann12", title = "Sega Genesis Launch",       description = "The Sega Genesis launched in North America on August 14, 1989.",date = dateOf(8, 14),  type = "ANNIVERSARY", emoji = "🌀")
)

fun dateOf(month: Int, day: Int): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.MONTH, month - 1)
    cal.set(Calendar.DAY_OF_MONTH, day)
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
    return cal.timeInMillis
}

fun eventTypeColor(type: String): Color = when (type) {
    "ANNIVERSARY" -> ScrapbookYellowDark
    "UPCOMING"    -> ScrapbookGreen
    "DLC"         -> ScrapbookBlue
    "CONVENTION"  -> ScrapbookRed
    "ESPORTS"     -> Color(0xFFBF5AF2)
    "COMMUNITY"   -> ScrapbookDark
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

// ─── Cover cache — fetches real covers from IGDB by exact title ───────────────

val eventCoverCache = mutableStateMapOf<String, String>()

@Composable
fun rememberEventCover(event: RetroEvent): String? {
    // Only UPCOMING and DLC have game art worth fetching
    if (event.type != "UPCOMING" && event.type != "DLC") return null

    val scope = rememberCoroutineScope()

    LaunchedEffect(event.id) {
        if (eventCoverCache.containsKey(event.id)) return@LaunchedEffect
        eventCoverCache[event.id] = "" // mark in-flight
        scope.launch {
            try {
                val results = IGDBRepository.searchGames(event.title)
                // Prefer exact title match, fall back to first result with a cover
                val match = results.firstOrNull {
                    it.coverUrl != null && it.name.equals(event.title, ignoreCase = true)
                } ?: results.firstOrNull { it.coverUrl != null }
                eventCoverCache[event.id] = match?.coverUrl ?: ""
            } catch (e: Exception) {
                eventCoverCache[event.id] = ""
            }
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
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val firebaseProfile by authViewModel.userProfile.collectAsState()

    var communityEvents by remember { mutableStateOf<List<RetroEvent>>(emptyList()) }
    var autoEvents by remember { mutableStateOf<List<RetroEvent>>(emptyList()) }
    var showAddEvent by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val today = remember { Calendar.getInstance() }
    var displayedMonth by remember { mutableStateOf(today.get(Calendar.MONTH)) }
    var displayedYear by remember { mutableStateOf(today.get(Calendar.YEAR)) }
    var selectedDay by remember { mutableStateOf(today.get(Calendar.DAY_OF_MONTH)) }

    val neonT = rememberInfiniteTransition(label = "eventsNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "eventsNeonAlpha")

    // Schedule background bi-weekly refresh
    LaunchedEffect(Unit) { scheduleEventsRefresh(context) }

    LaunchedEffect(Unit) {
        try {
            // Community events
            val docs = FirebaseFirestore.getInstance()
                .collection("events").orderBy("date", Query.Direction.ASCENDING).limit(50).get().await()
            communityEvents = docs.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                RetroEvent(
                    id = doc.id,
                    title = data["title"] as? String ?: "",
                    description = data["description"] as? String ?: "",
                    date = (data["date"] as? Long) ?: 0L,
                    type = data["type"] as? String ?: "COMMUNITY",
                    authorUid = data["authorUid"] as? String ?: "",
                    authorUsername = data["authorUsername"] as? String ?: "",
                    emoji = data["emoji"] as? String ?: "🎮"
                )
            }.filter { it.title.isNotBlank() }

            // Auto-fetched events from WorkManager results
            val autoDocs = FirebaseFirestore.getInstance()
                .collection("auto_events").limit(20).get().await()
            autoEvents = autoDocs.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val title = data["title"] as? String ?: return@mapNotNull null
                if (upcomingGameReleases.any { it.title.equals(title, ignoreCase = true) }) return@mapNotNull null
                RetroEvent(
                    id = "auto_${doc.id}",
                    title = title,
                    description = data["description"] as? String ?: "",
                    date = (data["date"] as? Long) ?: System.currentTimeMillis(),
                    type = "UPCOMING",
                    emoji = "🎮",
                    coverUrl = (data["coverUrl"] as? String)?.ifBlank { null }
                )
            }
        } catch (e: Exception) { }
    }

    val allUpcoming = remember(autoEvents) {
        (upcomingGameReleases + autoEvents).distinctBy { it.title.lowercase() }
    }

    val allEvents = remember(communityEvents, allUpcoming) {
        (retroAnniversaries + allUpcoming + upcomingDLCUpdates + gamingConventions + esportsEvents + communityEvents)
            .sortedBy { event ->
                val cal = Calendar.getInstance(); cal.timeInMillis = event.date
                cal.get(Calendar.MONTH) * 100 + cal.get(Calendar.DAY_OF_MONTH)
            }
    }

    val filteredEvents = remember(allEvents, selectedFilter) {
        if (selectedFilter == "ALL") allEvents else allEvents.filter { it.type == selectedFilter }
    }

    val eventsForSelectedDay = remember(filteredEvents, selectedDay, displayedMonth) {
        filteredEvents.filter { event ->
            val cal = Calendar.getInstance(); cal.timeInMillis = event.date
            cal.get(Calendar.MONTH) == displayedMonth && cal.get(Calendar.DAY_OF_MONTH) == selectedDay
        }
    }

    val eventsForMonth = remember(filteredEvents, displayedMonth) {
        filteredEvents.filter { event ->
            val cal = Calendar.getInstance(); cal.timeInMillis = event.date
            cal.get(Calendar.MONTH) == displayedMonth
        }
    }

    val daysWithEventTypes = remember(allEvents, displayedMonth) {
        val map = mutableMapOf<Int, MutableSet<String>>()
        allEvents.forEach { event ->
            val cal = Calendar.getInstance(); cal.timeInMillis = event.date
            if (cal.get(Calendar.MONTH) == displayedMonth) {
                map.getOrPut(cal.get(Calendar.DAY_OF_MONTH)) { mutableSetOf() }.add(event.type)
            }
        }
        map as Map<Int, Set<String>>
    }

    val todayEvents = remember(allEvents) {
        allEvents.filter { event ->
            val cal = Calendar.getInstance(); cal.timeInMillis = event.date
            cal.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    cal.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)
        }
    }

    if (showAddEvent) {
        AddEventSheet(
            authorUid = currentUser?.uid ?: "",
            authorUsername = firebaseProfile?.username ?: "",
            onDismiss = { showAddEvent = false },
            onSaved = { event -> communityEvents = communityEvents + event; showAddEvent = false }
        )
        return
    }

    Box(modifier = modifier.fillMaxSize().background(ScrapbookCream)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(colors = listOf(ScrapbookYellow, Color(0xFFFFE566), ScrapbookYellow)))
                    .border(BorderStroke(2.dp, ScrapbookBorder))
                    .padding(top = 16.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
            ) {
                val scanT = rememberInfiniteTransition(label = "scan")
                val scanX by scanT.animateFloat(initialValue = -400f, targetValue = 400f, animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "scanX")
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter).background(Brush.horizontalGradient(colors = listOf(Color.Transparent, ScrapbookDark.copy(alpha = 0.15f), Color.Transparent), startX = scanX, endX = scanX + 200f)))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        val shimmerT = rememberInfiniteTransition(label = "shimmer")
                        val shimmerX by shimmerT.animateFloat(initialValue = -300f, targetValue = 600f, animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart), label = "shimmerX")
                        Box {
                            Text("📅 EVENTS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 30.sp, letterSpacing = 2.sp)
                            Text("📅 EVENTS", fontFamily = BangersFontFamily, fontSize = 30.sp, letterSpacing = 2.sp, style = androidx.compose.ui.text.TextStyle(brush = Brush.linearGradient(colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.5f), Color.Transparent), start = androidx.compose.ui.geometry.Offset(shimmerX - 100f, 0f), end = androidx.compose.ui.geometry.Offset(shimmerX + 100f, 0f))))
                        }
                        Text("Gaming calendar & upcoming releases", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookDark).border(1.dp, ScrapbookBorder, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("${allEvents.size}", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (currentUser != null && firebaseProfile?.setupComplete == true) {
                        var addPressed by remember { mutableStateOf(false) }
                        val addScale by animateFloatAsState(targetValue = if (addPressed) 0.88f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "addScale")
                        Box(modifier = Modifier.scale(addScale).size(40.dp).clip(CircleShape).background(ScrapbookDark).border(2.dp, ScrapbookBorder, CircleShape).clickable { addPressed = true; showAddEvent = true }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Add, contentDescription = "Add event", tint = ScrapbookYellow, modifier = Modifier.size(20.dp))
                        }
                        LaunchedEffect(addPressed) { if (addPressed) { delay(150); addPressed = false } }
                    }
                }
            }

            // ── Filter chips ──────────────────────────────────────────────────
            LazyRow(modifier = Modifier.fillMaxWidth().background(ScrapbookDark).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val filters = listOf("ALL", "UPCOMING", "DLC", "CONVENTION", "ESPORTS", "ANNIVERSARY", "COMMUNITY")
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    val chipColor = if (filter == "ALL") ScrapbookYellow else eventTypeColor(filter)
                    var pressed by remember { mutableStateOf(false) }
                    val chipScale by animateFloatAsState(targetValue = if (pressed) 0.92f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "chip_$filter")
                    Box(
                        modifier = Modifier.scale(chipScale).clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) chipColor.copy(alpha = 0.15f) else ScrapbookCardWhite)
                            .border(width = if (isSelected) 1.5.dp else 1.dp, color = if (isSelected) chipColor else ScrapbookBorder, shape = RoundedCornerShape(20.dp))
                            .clickable { pressed = true; selectedFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = when (filter) {
                                "ALL" -> "✦ ALL"; "UPCOMING" -> "🎮 UPCOMING"; "DLC" -> "📦 DLC"
                                "CONVENTION" -> "🎪 SHOWS"; "ESPORTS" -> "🏅 ESPORTS"
                                "ANNIVERSARY" -> "🏆 RETRO"; "COMMUNITY" -> "👥 COMMUNITY"; else -> filter
                            },
                            fontFamily = BangersFontFamily,
                            color = if (isSelected) chipColor else ScrapbookDark,
                            fontSize = 12.sp
                        )
                    }
                    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {

                // ── MOST WANTED ───────────────────────────────────────────────
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookGreen.copy(alpha = neonAlpha)))
                        Text("🎮 MOST WANTED", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookGreen.copy(alpha = 0.15f)).border(1.dp, ScrapbookGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text("LIVE", fontFamily = BangersFontFamily, color = ScrapbookGreen, fontSize = 10.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(allUpcoming.sortedBy { it.date }.take(8), key = { it.id }) { game ->
                            UpcomingReleaseCard(event = game, neonAlpha = neonAlpha)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = ScrapbookBorder.copy(alpha = 0.15f))
                }

                // ── Today's highlight ─────────────────────────────────────────
                if (todayEvents.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { delay(200); visible = true }
                        val visAlpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(600), label = "visAlpha")
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).alpha(visAlpha), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
                                Text("TODAY'S HIGHLIGHT", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, modifier = Modifier.weight(1f))
                                val pulseT = rememberInfiniteTransition(label = "todayPulse")
                                val dotScale by pulseT.animateFloat(initialValue = 0.8f, targetValue = 1.3f, animationSpec = infiniteRepeatable(tween(700, easing = EaseInOut), RepeatMode.Reverse), label = "dot")
                                Box(modifier = Modifier.size(10.dp).scale(dotScale).clip(CircleShape).background(ScrapbookYellow))
                            }
                            todayEvents.forEach { event ->
                                EventCoverCard(event = event, neonAlpha = neonAlpha, height = 140.dp)
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
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
                            Text("📍 $monthName $selectedDay", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, modifier = Modifier.weight(1f))
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookDark).border(1.dp, ScrapbookYellow.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text("${eventsForSelectedDay.size}", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 13.sp)
                            }
                        }
                    }
                    items(eventsForSelectedDay, key = { "sel_${it.id}" }) { event ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            EventCoverCard(event = event, neonAlpha = neonAlpha)
                        }
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }

                // ── This month ────────────────────────────────────────────────
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
                        Text("THIS MONTH", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, modifier = Modifier.weight(1f))
                        HorizontalDivider(modifier = Modifier.weight(1f), color = ScrapbookBorder.copy(alpha = 0.2f))
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookDark).border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text("${eventsForMonth.size}", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 13.sp)
                        }
                    }
                }
                if (eventsForMonth.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📭", fontSize = 40.sp)
                                Text("NO EVENTS THIS MONTH", fontFamily = BangersFontFamily, color = ScrapbookTextMuted, fontSize = 16.sp)
                                Text("Try a different filter above", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    items(eventsForMonth, key = { "month_${it.id}" }) { event ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { EventCoverCard(event = event, neonAlpha = neonAlpha) }
                    }
                }

                // ── Upcoming Releases ─────────────────────────────────────────
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookGreen.copy(alpha = neonAlpha)))
                        Text("UPCOMING RELEASES", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, modifier = Modifier.weight(1f))
                        if (autoEvents.isNotEmpty()) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookGreen.copy(alpha = 0.12f)).border(1.dp, ScrapbookGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text("+${autoEvents.size} NEW", fontFamily = BangersFontFamily, color = ScrapbookGreen, fontSize = 10.sp)
                            }
                        }
                    }
                }
                items(allUpcoming.sortedBy { it.date }, key = { "ugr_${it.id}" }) { event ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { EventCoverCard(event = event, neonAlpha = neonAlpha) }
                }

                // ── DLC & Updates ─────────────────────────────────────────────
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookBlue.copy(alpha = neonAlpha)))
                        Text("DLC & UPDATES", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, modifier = Modifier.weight(1f))
                    }
                }
                items(upcomingDLCUpdates.sortedBy { it.date }, key = { "dlc_${it.id}" }) { event ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { EventCoverCard(event = event, neonAlpha = neonAlpha) }
                }

                // ── Conventions ───────────────────────────────────────────────
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookRed.copy(alpha = neonAlpha)))
                        Text("GAMING CONVENTIONS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, modifier = Modifier.weight(1f))
                    }
                }
                items(gamingConventions.sortedBy { it.date }, key = { "con_${it.id}" }) { event ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { EventCoverCard(event = event, neonAlpha = neonAlpha) }
                }

                // ── Esports ───────────────────────────────────────────────────
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFBF5AF2).copy(alpha = neonAlpha)))
                        Text("ESPORTS CHAMPIONSHIPS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, modifier = Modifier.weight(1f))
                    }
                }
                items(esportsEvents.sortedBy { it.date }, key = { "esp_${it.id}" }) { event ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { EventCoverCard(event = event, neonAlpha = neonAlpha) }
                }

                // ── Retro Anniversaries ───────────────────────────────────────
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellowDark.copy(alpha = neonAlpha)))
                        Text("RETRO ANNIVERSARIES", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, modifier = Modifier.weight(1f))
                    }
                }
                items(retroAnniversaries, key = { "ann_${it.id}" }) { event ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { EventCoverCard(event = event, neonAlpha = neonAlpha) }
                }
            }
        }
    }
}

// ─── Upcoming Release Card (horizontal strip) ─────────────────────────────────

@Composable
fun UpcomingReleaseCard(event: RetroEvent, neonAlpha: Float) {
    val fetchedCover = rememberEventCover(event)
    val coverUrl = event.coverUrl ?: fetchedCover
    val typeColor = ScrapbookGreen
    val dateStr = remember(event.date) { SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(event.date)) }
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.94f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "upScale")

    Box(
        modifier = Modifier.width(150.dp).height(200.dp).scale(cardScale)
            .clip(RoundedCornerShape(14.dp))
            .border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(typeColor.copy(alpha = neonAlpha), typeColor.copy(alpha = 0.2f), typeColor.copy(alpha = neonAlpha))), shape = RoundedCornerShape(14.dp))
            .clickable { pressed = true }
    ) {
        if (coverUrl != null) {
            AsyncImage(model = coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1C1C2E)))
            val shimmerT = rememberInfiniteTransition(label = "shimUp_${event.id}")
            val shimmerX by shimmerT.animateFloat(initialValue = -300f, targetValue = 300f, animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), label = "shimX")
            Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0f)), start = androidx.compose.ui.geometry.Offset(shimmerX, 0f), end = androidx.compose.ui.geometry.Offset(shimmerX + 150f, 200f))))
        }
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))))
        Column(modifier = Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(typeColor.copy(alpha = 0.9f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text("NEW", fontFamily = BangersFontFamily, color = Color.White, fontSize = 9.sp, letterSpacing = 1.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(event.title, fontFamily = BangersFontFamily, color = Color.White, fontSize = 13.sp, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                    Text(dateStr, fontFamily = BangersFontFamily, color = typeColor, fontSize = 11.sp)
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Event Cover Card ─────────────────────────────────────────────────────────

@Composable
fun EventCoverCard(event: RetroEvent, neonAlpha: Float, height: androidx.compose.ui.unit.Dp = 120.dp) {
    val fetchedCover = rememberEventCover(event)
    val coverUrl = event.coverUrl ?: fetchedCover
    val typeColor = eventTypeColor(event.type)
    val dateStr = remember(event.date) { SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(event.date)) }
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "coverCardScale")

    Box(
        modifier = Modifier.fillMaxWidth().height(height).scale(cardScale)
            .clip(RoundedCornerShape(12.dp))
            .border(width = 1.5.dp, brush = Brush.linearGradient(colors = listOf(typeColor.copy(alpha = neonAlpha * 0.6f), typeColor.copy(alpha = 0.1f), typeColor.copy(alpha = neonAlpha * 0.6f))), shape = RoundedCornerShape(12.dp))
            .clickable { pressed = true }
    ) {
        if (coverUrl != null) {
            AsyncImage(model = coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), alpha = 0.65f)
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(colors = listOf(typeColor.copy(alpha = 0.25f), ScrapbookDark.copy(alpha = 0.95f)))))
            if (event.type == "UPCOMING" || event.type == "DLC") {
                val shimmerT = rememberInfiniteTransition(label = "shimCover_${event.id}")
                val shimmerX by shimmerT.animateFloat(initialValue = -400f, targetValue = 400f, animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "shimX")
                Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.05f), Color.Transparent), start = androidx.compose.ui.geometry.Offset(shimmerX, 0f), end = androidx.compose.ui.geometry.Offset(shimmerX + 200f, 0f))))
            }
        }
        Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(colors = listOf(Color.Black.copy(alpha = 0.78f), Color.Black.copy(alpha = 0.25f)))))
        Row(modifier = Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(typeColor.copy(alpha = 0.9f)).padding(horizontal = 7.dp, vertical = 3.dp)) {
                    Text(eventTypeLabel(event.type), fontFamily = BangersFontFamily, color = Color.White, fontSize = 9.sp)
                }
                Text(event.title, fontFamily = BangersFontFamily, color = Color.White, fontSize = 16.sp, lineHeight = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(event.description, fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
                if (event.authorUsername.isNotBlank()) {
                    Text("by ${event.authorUsername}", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.55f)).border(1.dp, typeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(dateStr.split(" ").getOrElse(0) { "" }.uppercase(), fontFamily = BangersFontFamily, color = typeColor, fontSize = 10.sp, letterSpacing = 1.sp)
                Text(dateStr.split(" ").getOrElse(1) { "" }, fontFamily = BangersFontFamily, color = Color.White, fontSize = 20.sp, lineHeight = 22.sp)
            }
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
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth().border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = RoundedCornerShape(14.dp)),
            backgroundColor = ScrapbookCardWhite, cornerRadius = 14.dp, shadowOffset = 4.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    var prevPressed by remember { mutableStateOf(false) }
                    val prevScale by animateFloatAsState(targetValue = if (prevPressed) 0.88f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "prevScale")
                    Box(modifier = Modifier.scale(prevScale).size(34.dp).clip(CircleShape).background(ScrapbookPaper).border(2.dp, ScrapbookBorder, CircleShape).clickable { prevPressed = true; onPrevMonth() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(16.dp))
                    }
                    LaunchedEffect(prevPressed) { if (prevPressed) { delay(150); prevPressed = false } }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(monthName.uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 17.sp, letterSpacing = 1.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            listOf("ANN" to ScrapbookYellowDark, "NEW" to ScrapbookGreen, "DLC" to ScrapbookBlue, "CON" to ScrapbookRed, "ESP" to Color(0xFFBF5AF2)).forEach { (label, color) ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(color))
                                    Text(label, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 7.sp)
                                }
                            }
                        }
                    }

                    var nextPressed by remember { mutableStateOf(false) }
                    val nextScale by animateFloatAsState(targetValue = if (nextPressed) 0.88f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "nextScale")
                    Box(modifier = Modifier.scale(nextScale).size(34.dp).clip(CircleShape).background(ScrapbookPaper).border(2.dp, ScrapbookBorder, CircleShape).clickable { nextPressed = true; onNextMonth() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(16.dp))
                    }
                    LaunchedEffect(nextPressed) { if (nextPressed) { delay(150); nextPressed = false } }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { day ->
                        Text(day, fontFamily = BangersFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

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
                                val isToday = day == today.get(Calendar.DAY_OF_MONTH) && displayedMonth == today.get(Calendar.MONTH) && displayedYear == today.get(Calendar.YEAR)
                                val isSelected = day == selectedDay
                                val eventTypes = daysWithEventTypes[day] ?: emptySet()
                                var dayPressed by remember { mutableStateOf(false) }
                                val dayScale by animateFloatAsState(targetValue = if (dayPressed) 0.85f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "day_$day")

                                Box(
                                    modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp).scale(dayScale)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(when {
                                            isSelected -> ScrapbookDark
                                            isToday -> ScrapbookYellow.copy(alpha = 0.25f)
                                            eventTypes.isNotEmpty() -> ScrapbookPaper
                                            else -> Color.Transparent
                                        })
                                        .then(if (isToday && !isSelected) Modifier.border(1.5.dp, ScrapbookYellow, RoundedCornerShape(8.dp)) else Modifier)
                                        .clickable { dayPressed = true; onDaySelected(day) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(2.dp)) {
                                        Text(
                                            text = "$day",
                                            fontFamily = if (isSelected || isToday) BangersFontFamily else NunitoFontFamily,
                                            color = when {
                                                isSelected -> ScrapbookYellow
                                                isToday -> ScrapbookDark
                                                eventTypes.isNotEmpty() -> ScrapbookDark
                                                else -> ScrapbookDark.copy(alpha = 0.4f)
                                            },
                                            fontSize = 13.sp,
                                            fontWeight = if (eventTypes.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (eventTypes.isNotEmpty()) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 1.dp)) {
                                                eventTypes.take(3).forEach { type ->
                                                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(if (isSelected) ScrapbookYellow.copy(alpha = 0.7f) else eventTypeColor(type)))
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
    val neonT = rememberInfiniteTransition(label = "sheetNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "sheetNeonAlpha")
    val isValid = title.isNotBlank()

    Box(modifier = Modifier.fillMaxSize().background(ScrapbookCream)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(ScrapbookYellow, Color(0xFFFFE566), ScrapbookYellow))).border(BorderStroke(2.dp, ScrapbookBorder)).padding(top = 16.dp, bottom = 12.dp, start = 4.dp, end = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Close", tint = ScrapbookDark) }
                    Text("ADD EVENT", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 26.sp, modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        .background(if (isValid) ScrapbookDark else ScrapbookDark.copy(alpha = 0.3f))
                        .border(width = 2.dp, brush = if (isValid) Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha))) else Brush.linearGradient(colors = listOf(ScrapbookBorder, ScrapbookBorder)), shape = RoundedCornerShape(10.dp))
                        .clickable(enabled = isValid && !isSaving) {
                            isSaving = true
                            val date = dateOf(selectedMonth, selectedDay)
                            val event = RetroEvent(id = System.currentTimeMillis().toString(), title = title, description = description, date = date, type = "COMMUNITY", authorUid = authorUid, authorUsername = authorUsername, emoji = selectedEmoji)
                            FirebaseFirestore.getInstance().collection("events").document(event.id)
                                .set(mapOf("title" to event.title, "description" to event.description, "date" to event.date, "type" to event.type, "authorUid" to event.authorUid, "authorUsername" to event.authorUsername, "emoji" to event.emoji))
                                .addOnSuccessListener { onSaved(event) }
                                .addOnFailureListener { isSaving = false }
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        if (isSaving) CircularProgressIndicator(color = ScrapbookYellow, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text("SAVE", fontFamily = BangersFontFamily, color = if (isValid) ScrapbookYellow else ScrapbookYellow.copy(alpha = 0.3f), fontSize = 16.sp)
                    }
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp, shadowOffset = 3.dp) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.width(4.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
                                Text("PICK AN EMOJI", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                emojiOptions.forEach { emoji ->
                                    val isSel = selectedEmoji == emoji
                                    var ep by remember { mutableStateOf(false) }
                                    val es by animateFloatAsState(targetValue = if (ep) 0.85f else if (isSel) 1.15f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "e_$emoji")
                                    Box(modifier = Modifier.scale(es).size(36.dp).clip(RoundedCornerShape(8.dp)).background(if (isSel) ScrapbookYellow else ScrapbookPaper).border(width = if (isSel) 2.dp else 1.dp, color = if (isSel) ScrapbookBorder else ScrapbookBorder.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp)).clickable { ep = true; selectedEmoji = emoji }, contentAlignment = Alignment.Center) { Text(emoji, fontSize = 17.sp) }
                                    LaunchedEffect(ep) { if (ep) { delay(150); ep = false } }
                                }
                            }
                        }
                    }
                }
                item {
                    ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp, shadowOffset = 3.dp) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.width(4.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
                                Text("EVENT TITLE *", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                            }
                            OutlinedTextField(value = title, onValueChange = { title = it }, placeholder = { Text("e.g. Mario Kart Night", fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookTextMuted) }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextDark), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScrapbookYellow, unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f), focusedContainerColor = ScrapbookCardWhite, unfocusedContainerColor = ScrapbookCardWhite, cursorColor = ScrapbookDark, focusedTextColor = ScrapbookTextDark, unfocusedTextColor = ScrapbookTextDark), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                item {
                    ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp, shadowOffset = 3.dp) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.width(4.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
                                Text("DESCRIPTION", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                            }
                            OutlinedTextField(value = description, onValueChange = { description = it }, placeholder = { Text("What's this event about?", fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookTextMuted) }, minLines = 3, textStyle = androidx.compose.ui.text.TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextDark), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScrapbookYellow, unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f), focusedContainerColor = ScrapbookCardWhite, unfocusedContainerColor = ScrapbookCardWhite, cursorColor = ScrapbookDark, focusedTextColor = ScrapbookTextDark, unfocusedTextColor = ScrapbookTextDark), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                item {
                    ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp, shadowOffset = 3.dp) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.width(4.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
                                Text("SELECT DATE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("MONTH", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(value = "$selectedMonth", onValueChange = { selectedMonth = it.toIntOrNull()?.coerceIn(1, 12) ?: selectedMonth }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextDark), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScrapbookYellow, unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f), focusedContainerColor = ScrapbookCardWhite, unfocusedContainerColor = ScrapbookCardWhite, cursorColor = ScrapbookDark), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth())
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("DAY", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(value = "$selectedDay", onValueChange = { selectedDay = it.toIntOrNull()?.coerceIn(1, 31) ?: selectedDay }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextDark), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScrapbookYellow, unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f), focusedContainerColor = ScrapbookCardWhite, unfocusedContainerColor = ScrapbookCardWhite, cursorColor = ScrapbookDark), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth())
                                }
                            }
                            val previewDate = try { dateOf(selectedMonth, selectedDay) } catch (e: Exception) { 0L }
                            if (previewDate > 0L) {
                                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(ScrapbookYellow.copy(alpha = 0.15f)).border(1.dp, ScrapbookYellowDark.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("📅", fontSize = 16.sp)
                                    Text("Event on: ${SimpleDateFormat("MMMM d", Locale.getDefault()).format(Date(previewDate))}", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
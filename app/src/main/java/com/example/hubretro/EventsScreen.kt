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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hubretro.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class RetroEvent(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val date: Long = 0L,
    val type: String = "COMMUNITY", // ANNIVERSARY or COMMUNITY
    val authorUid: String = "",
    val authorUsername: String = "",
    val emoji: String = "🎮"
)

// Hardcoded retro gaming anniversaries
val retroAnniversaries = listOf(
    RetroEvent(id = "ann1", title = "Super Mario Bros Launch", description = "Super Mario Bros was released for the NES in Japan, revolutionizing gaming forever.", date = dateOf(9, 13), type = "ANNIVERSARY", emoji = "🍄"),
    RetroEvent(id = "ann2", title = "Game Boy Birthday", description = "Nintendo's Game Boy was released in Japan on April 21, 1989.", date = dateOf(4, 21), type = "ANNIVERSARY", emoji = "🎮"),
    RetroEvent(id = "ann3", title = "Sonic the Hedgehog Launch", description = "Sonic the Hedgehog debuted on the Sega Genesis on June 23, 1991.", date = dateOf(6, 23), type = "ANNIVERSARY", emoji = "💨"),
    RetroEvent(id = "ann4", title = "PlayStation 1 Launch", description = "Sony launched the original PlayStation in Japan on December 3, 1994.", date = dateOf(12, 3), type = "ANNIVERSARY", emoji = "🎯"),
    RetroEvent(id = "ann5", title = "Zelda: Ocarina of Time", description = "The Legend of Zelda: Ocarina of Time released on November 21, 1998.", date = dateOf(11, 21), type = "ANNIVERSARY", emoji = "🗡️"),
    RetroEvent(id = "ann6", title = "Pac-Man Arcade Debut", description = "Pac-Man first appeared in arcades in Japan on May 22, 1980.", date = dateOf(5, 22), type = "ANNIVERSARY", emoji = "👾"),
    RetroEvent(id = "ann7", title = "Tetris Day", description = "Tetris was released on June 6, 1984 by Alexey Pajitnov.", date = dateOf(6, 6), type = "ANNIVERSARY", emoji = "🧩"),
    RetroEvent(id = "ann8", title = "Doom Release Day", description = "id Software released Doom as shareware on December 10, 1993.", date = dateOf(12, 10), type = "ANNIVERSARY", emoji = "🔫"),
    RetroEvent(id = "ann9", title = "Nintendo NES Launch", description = "The NES launched in North America on October 18, 1985.", date = dateOf(10, 18), type = "ANNIVERSARY", emoji = "🕹️"),
    RetroEvent(id = "ann10", title = "Street Fighter II Launch", description = "Street Fighter II hit arcades on February 6, 1991.", date = dateOf(2, 6), type = "ANNIVERSARY", emoji = "🥊"),
    RetroEvent(id = "ann11", title = "Pokemon Red & Blue Launch", description = "Pokémon Red and Blue launched in the US on September 28, 1998.", date = dateOf(9, 28), type = "ANNIVERSARY", emoji = "⚡"),
    RetroEvent(id = "ann12", title = "Sega Genesis Launch", description = "The Sega Genesis launched in North America on August 14, 1989.", date = dateOf(8, 14), type = "ANNIVERSARY", emoji = "🌀")
)

fun dateOf(month: Int, day: Int): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.MONTH, month - 1)
    cal.set(Calendar.DAY_OF_MONTH, day)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    return cal.timeInMillis
}

@Composable
fun EventsScreen(
    authViewModel: AuthViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val firebaseProfile by authViewModel.userProfile.collectAsState()

    var communityEvents by remember { mutableStateOf<List<RetroEvent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddEvent by remember { mutableStateOf(false) }

    // Calendar state
    val today = remember { Calendar.getInstance() }
    var displayedMonth by remember { mutableStateOf(today.get(Calendar.MONTH)) }
    var displayedYear by remember { mutableStateOf(today.get(Calendar.YEAR)) }
    var selectedDay by remember { mutableStateOf(today.get(Calendar.DAY_OF_MONTH)) }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val docs = FirebaseFirestore.getInstance()
                .collection("events")
                .orderBy("date", Query.Direction.ASCENDING)
                .limit(50)
                .get().await()
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
        } catch (e: Exception) { } finally {
            isLoading = false
        }
    }

    // All events combined
    val allEvents = remember(communityEvents) {
        (retroAnniversaries + communityEvents).sortedBy { event ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = event.date
            cal.get(Calendar.MONTH) * 100 + cal.get(Calendar.DAY_OF_MONTH)
        }
    }

    // Events for selected day
    val eventsForSelectedDay = remember(allEvents, selectedDay, displayedMonth) {
        allEvents.filter { event ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = event.date
            cal.get(Calendar.MONTH) == displayedMonth &&
                    cal.get(Calendar.DAY_OF_MONTH) == selectedDay
        }
    }

    // Events for displayed month
    val eventsForMonth = remember(allEvents, displayedMonth) {
        allEvents.filter { event ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = event.date
            cal.get(Calendar.MONTH) == displayedMonth
        }
    }

    // Days with events
    val daysWithEvents = remember(allEvents, displayedMonth) {
        allEvents.mapNotNull { event ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = event.date
            if (cal.get(Calendar.MONTH) == displayedMonth)
                cal.get(Calendar.DAY_OF_MONTH)
            else null
        }.toSet()
    }

    if (showAddEvent) {
        AddEventSheet(
            authorUid = currentUser?.uid ?: "",
            authorUsername = firebaseProfile?.username ?: "",
            onDismiss = { showAddEvent = false },
            onSaved = { event ->
                communityEvents = communityEvents + event
                showAddEvent = false
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
                    Text(
                        text = "📅 EVENTS",
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 28.sp,
                        letterSpacing = 2.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (currentUser != null && firebaseProfile?.setupComplete == true) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(ScrapbookDark)
                                .border(2.dp, ScrapbookBorder, CircleShape)
                                .clickable { showAddEvent = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Add event",
                                tint = ScrapbookYellow,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Calendar
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    RetroCalendar(
                        displayedMonth = displayedMonth,
                        displayedYear = displayedYear,
                        selectedDay = selectedDay,
                        daysWithEvents = daysWithEvents,
                        today = today,
                        onDaySelected = { day -> selectedDay = day },
                        onPrevMonth = {
                            if (displayedMonth == 0) {
                                displayedMonth = 11; displayedYear--
                            } else displayedMonth--
                        },
                        onNextMonth = {
                            if (displayedMonth == 11) {
                                displayedMonth = 0; displayedYear++
                            } else displayedMonth++
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Selected day events
                if (eventsForSelectedDay.isNotEmpty()) {
                    item {
                        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).let {
                            val cal = Calendar.getInstance()
                            cal.set(Calendar.MONTH, displayedMonth)
                            it.format(cal.time)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📍 $monthName $selectedDay",
                                fontFamily = BangersFontFamily,
                                color = ScrapbookDark,
                                fontSize = 20.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Divider(
                                modifier = Modifier.weight(1f),
                                color = ScrapbookBorder.copy(alpha = 0.2f)
                            )
                        }
                    }
                    items(eventsForSelectedDay, key = { "sel_${it.id}" }) { event ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            EventCard(event = event)
                        }
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }

                // This month events
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "THIS MONTH",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 20.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Divider(
                            modifier = Modifier.weight(1f),
                            color = ScrapbookBorder.copy(alpha = 0.2f)
                        )
                    }
                }

                if (eventsForMonth.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No events this month",
                                fontFamily = NunitoFontFamily,
                                color = ScrapbookTextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(eventsForMonth, key = { "month_${it.id}" }) { event ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            EventCard(event = event)
                        }
                    }
                }

                // All upcoming
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ALL RETRO ANNIVERSARIES",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 20.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                items(retroAnniversaries, key = { "all_${it.id}" }) { event ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        EventCard(event = event)
                    }
                }
            }
        }
    }
}

@Composable
fun RetroCalendar(
    displayedMonth: Int,
    displayedYear: Int,
    selectedDay: Int,
    daysWithEvents: Set<Int>,
    today: Calendar,
    onDaySelected: (Int) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthName = remember(displayedMonth, displayedYear) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, displayedMonth)
        cal.set(Calendar.YEAR, displayedYear)
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    val daysInMonth = remember(displayedMonth, displayedYear) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, displayedMonth)
        cal.set(Calendar.YEAR, displayedYear)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val firstDayOfWeek = remember(displayedMonth, displayedYear) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, displayedMonth)
        cal.set(Calendar.YEAR, displayedYear)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        (cal.get(Calendar.DAY_OF_WEEK) - 2).let { if (it < 0) 6 else it }
    }

    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 14.dp,
            shadowOffset = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Month nav
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrevMonth) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Previous month",
                            tint = ScrapbookDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = monthName.uppercase(),
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 20.sp,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onNextMonth) {
                        Icon(
                            Icons.Filled.ArrowForward,
                            contentDescription = "Next month",
                            tint = ScrapbookDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Day labels
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { day ->
                        Text(
                            text = day,
                            fontFamily = BangersFontFamily,
                            color = ScrapbookTextMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Days grid
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
                                val isToday = day == today.get(Calendar.DAY_OF_MONTH) &&
                                        displayedMonth == today.get(Calendar.MONTH) &&
                                        displayedYear == today.get(Calendar.YEAR)
                                val isSelected = day == selectedDay
                                val hasEvent = daysWithEvents.contains(day)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> ScrapbookDark
                                                isToday -> ScrapbookYellow
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable { onDaySelected(day) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$day",
                                            fontFamily = if (isSelected || isToday) BangersFontFamily
                                            else NunitoFontFamily,
                                            color = when {
                                                isSelected -> ScrapbookYellow
                                                isToday -> ScrapbookDark
                                                else -> ScrapbookDark
                                            },
                                            fontSize = 13.sp,
                                            fontWeight = if (hasEvent) FontWeight.Bold
                                            else FontWeight.Normal
                                        )
                                        if (hasEvent) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) ScrapbookYellow
                                                        else ScrapbookYellowDark
                                                    )
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

@Composable
fun EventCard(event: RetroEvent) {
    val dateStr = remember(event.date) {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(event.date))
    }
    Box {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = if (event.type == "ANNIVERSARY")
                ScrapbookYellow.copy(alpha = 0.15f)
            else ScrapbookCardWhite,
            borderColor = if (event.type == "ANNIVERSARY")
                ScrapbookYellowDark
            else ScrapbookBorder,
            cornerRadius = 12.dp,
            shadowOffset = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Date box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ScrapbookDark)
                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dateStr.split(" ").getOrElse(0) { "" }.uppercase(),
                            fontFamily = BangersFontFamily,
                            color = ScrapbookYellow,
                            fontSize = 10.sp
                        )
                        Text(
                            text = dateStr.split(" ").getOrElse(1) { "" },
                            fontFamily = BangersFontFamily,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(event.emoji, fontSize = 16.sp)
                        Text(
                            text = event.title,
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 20.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.description,
                        fontFamily = NunitoFontFamily,
                        color = ScrapbookTextMuted,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (event.type == "ANNIVERSARY") ScrapbookYellow
                                else ScrapbookPaper
                            )
                            .border(1.dp, ScrapbookBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (event.type == "ANNIVERSARY") "🏆 ANNIVERSARY"
                            else "👥 COMMUNITY",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddEventSheet(
    authorUid: String,
    authorUsername: String,
    onDismiss: () -> Unit,
    onSaved: (RetroEvent) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🎮") }
    var isSaving by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }
    var selectedDay by remember { mutableStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }

    val emojiOptions = listOf("🎮", "🕹️", "👾", "🏆", "🎯", "💾", "📺", "🎲", "⭐", "🔥")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScrapbookCream)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = ScrapbookDark
                        )
                    }
                    Text(
                        text = "ADD EVENT",
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 26.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (title.isNotBlank()) ScrapbookDark
                                else ScrapbookDark.copy(alpha = 0.3f)
                            )
                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                            .clickable(enabled = title.isNotBlank() && !isSaving) {
                                isSaving = true
                                val date = dateOf(selectedMonth, selectedDay)
                                val event = RetroEvent(
                                    id = System.currentTimeMillis().toString(),
                                    title = title,
                                    description = description,
                                    date = date,
                                    type = "COMMUNITY",
                                    authorUid = authorUid,
                                    authorUsername = authorUsername,
                                    emoji = selectedEmoji
                                )
                                // Save to Firestore
                                FirebaseFirestore.getInstance()
                                    .collection("events")
                                    .document(event.id)
                                    .set(mapOf(
                                        "title" to event.title,
                                        "description" to event.description,
                                        "date" to event.date,
                                        "type" to event.type,
                                        "authorUid" to event.authorUid,
                                        "authorUsername" to event.authorUsername,
                                        "emoji" to event.emoji
                                    ))
                                    .addOnSuccessListener { onSaved(event) }
                                    .addOnFailureListener { isSaving = false }
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
                            Text(
                                "SAVE",
                                fontFamily = BangersFontFamily,
                                color = ScrapbookYellow,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Emoji picker
                    Text("EMOJI", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        emojiOptions.forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selectedEmoji == emoji) ScrapbookYellow
                                        else ScrapbookPaper
                                    )
                                    .border(2.dp, ScrapbookBorder, CircleShape)
                                    .clickable { selectedEmoji = emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 18.sp)
                            }
                        }
                    }
                }
                item {
                    ScrapbookInputField(
                        value = title,
                        onValueChange = { title = it },
                        label = "EVENT TITLE"
                    )
                }
                item {
                    ScrapbookInputField(
                        value = description,
                        onValueChange = { description = it },
                        label = "DESCRIPTION"
                    )
                }
                item {
                    Text("DATE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("MONTH", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                            OutlinedTextField(
                                value = "$selectedMonth",
                                onValueChange = { selectedMonth = it.toIntOrNull()?.coerceIn(1, 12) ?: selectedMonth },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextDark),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ScrapbookDark,
                                    unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f),
                                    focusedContainerColor = ScrapbookCardWhite,
                                    unfocusedContainerColor = ScrapbookCardWhite,
                                    cursorColor = ScrapbookDark
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("DAY", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                            OutlinedTextField(
                                value = "$selectedDay",
                                onValueChange = { selectedDay = it.toIntOrNull()?.coerceIn(1, 31) ?: selectedDay },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextDark),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ScrapbookDark,
                                    unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f),
                                    focusedContainerColor = ScrapbookCardWhite,
                                    unfocusedContainerColor = ScrapbookCardWhite,
                                    cursorColor = ScrapbookDark
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
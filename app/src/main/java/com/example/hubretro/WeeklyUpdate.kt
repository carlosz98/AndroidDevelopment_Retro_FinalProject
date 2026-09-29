package com.example.hubretro

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ═══════════════════════════════════════════════════════════════════════════════
// WEEKLY UPDATE — pulled fresh every week (and on demand):
//   • games releasing in the next 7 days (IGDB, main games / remakes / ports)
//   • DLC, expansions, packs & big updates in the same window
//   • top gaming + retro stories from the last 7 days (RSS feeds)
//   • community articles posted in the last 7 days (Firestore)
// Shown as a card on Home + Events, a full sheet, and a weekly notification.
// ═══════════════════════════════════════════════════════════════════════════════

object WeeklyDigestBus { var open by mutableStateOf(false) }

data class WeeklyDigestData(
    val weekStart: Long,
    val games: List<IGDBUpcomingGame>,
    val dlc: List<IGDBUpcomingGame>,
    val news: List<ArticleItem>,
    val community: List<ArticleItem>,
    val builtAt: Long
) {
    val isEmpty get() = games.isEmpty() && dlc.isEmpty() && news.isEmpty() && community.isEmpty()
}

object WeeklyDigest {
    private val MAIN_TYPES = setOf(0, 8, 9, 10, 11)
    @Volatile var cached: WeeklyDigestData? = null
        private set

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    suspend fun build(force: Boolean = false): WeeklyDigestData {
        val now = System.currentTimeMillis()
        cached?.let { if (!force && now - it.builtAt < 30 * 60_000L) return it }
        if (force) IGDBRepository.clearReleaseCache()

        val start = startOfToday()
        val end = start + 7 * 86_400_000L
        val weekAgo = now - 7 * 86_400_000L

        val releases = runCatching { IGDBRepository.fetchReleasesBetween(start, end) }.getOrDefault(emptyList())
        val news = runCatching { LiveFeeds.fetchAll(force) }.getOrDefault(emptyList())
            .filter { it.publishedAt >= weekAgo }
            .sortedWith(compareByDescending<ArticleItem> { it.category == "RETRO" }.thenByDescending { it.publishedAt })
            .take(12)
        val community = runCatching {
            FirebaseFirestore.getInstance().collection("articles")
                .whereGreaterThan("timestamp", Timestamp(Date(weekAgo)))
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10).get().await()
                .documents.mapNotNull { d ->
                    val title = d.getString("title") ?: return@mapNotNull null
                    val ts = d.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                    ArticleItem(
                        id = d.id, title = title,
                        snippet = d.getString("snippet") ?: "",
                        author = d.getString("authorUsername") ?: "Anonymous",
                        authorUid = d.getString("authorUid"),
                        imageUrl = d.getString("headerImageUrl"),
                        category = d.getString("category") ?: "GAMING",
                        date = LiveFeeds.friendlyDate(ts),
                        publishedAt = ts
                    )
                }
        }.getOrDefault(emptyList())

        val data = WeeklyDigestData(
            weekStart = start,
            games = releases.filter { it.category in MAIN_TYPES },
            dlc = releases.filter { it.category !in MAIN_TYPES },
            news = news,
            community = community,
            builtAt = now
        )
        cached = data
        return data
    }

    fun rangeLabel(start: Long): String {
        val f = SimpleDateFormat("MMM d", Locale.getDefault())
        return "${f.format(Date(start))} – ${f.format(Date(start + 6 * 86_400_000L))}"
    }

    fun weekNumber(start: Long): Int = Calendar.getInstance().apply { timeInMillis = start }.get(Calendar.WEEK_OF_YEAR)
}

// ─── Card (Home + Events) ─────────────────────────────────────────────────────

@Composable
fun WeeklyUpdateCard(modifier: Modifier = Modifier) {
    var digest by remember { mutableStateOf(WeeklyDigest.cached) }
    LaunchedEffect(Unit) { digest = WeeklyDigest.build() }
    val d = digest
    val tap = rememberTapHaptic()

    Box(modifier = modifier.fillMaxWidth().dealIn(0, 4)) {
        Box(Modifier.matchParentSize().offset(4.dp, 4.dp).clip(RoundedCornerShape(16.dp)).background(ScrapbookDark))
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp))
                .clickable { tap(); Chiptune.play(Chiptune.Sfx.POP); WeeklyDigestBus.open = true }
        ) {
            // header strip
            Row(
                modifier = Modifier.fillMaxWidth().background(CGreen).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🗓️ WEEKLY UPDATE", modifier = Modifier.weight(1f),
                    style = TextStyle(fontFamily = BangersFontFamily, fontSize = 18.sp, letterSpacing = 1.sp, color = Color.White))
                Text(
                    if (d != null) "WEEK ${WeeklyDigest.weekNumber(d.weekStart)} · ${WeeklyDigest.rangeLabel(d.weekStart)}" else "PULLING…",
                    style = TextStyle(fontFamily = BangersFontFamily, fontSize = 12.sp, letterSpacing = 0.5.sp, color = Color.White.copy(alpha = 0.9f))
                )
            }
            if (d == null) {
                NowLoadingIndicator(label = "PULLING THIS WEEK")
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CoverStack(d.games.ifEmpty { d.dlc }.mapNotNull { it.coverUrl }.take(4))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            DigestStat(d.games.size, "GAMES", Modifier.weight(1f))
                            DigestStat(d.dlc.size, "DLC", Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            DigestStat(d.news.size, "STORIES", Modifier.weight(1f))
                            DigestStat(d.community.size, "COMMUNITY", Modifier.weight(1f))
                        }
                    }
                }
                val headline = d.games.firstOrNull()?.let { "🎮 ${it.name} leads the week" }
                    ?: d.news.firstOrNull()?.let { "📰 ${it.title}" }
                    ?: "Quiet week — check the calendar for what's next"
                Row(
                    modifier = Modifier.fillMaxWidth().background(ComicGlassBg).padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(headline, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 12.sp, color = ScrapbookDark))
                    ArcadeBlinkText("OPEN ▶", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 13.sp, letterSpacing = 1.sp, color = CGreenDeep))
                }
            }
        }
    }
}

@Composable
private fun DigestStat(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(ComicGlassBg)
            .border(1.5.dp, ScrapbookDark, RoundedCornerShape(8.dp)).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RollingCounterText(value.toString(), TextStyle(fontFamily = BangersFontFamily, fontSize = 18.sp, color = CGreenDeep))
        Text(label, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 10.sp, letterSpacing = 1.sp, color = ScrapbookTextMuted))
    }
}

/** Fanned stack of cover art, dealt in like cards. */
@Composable
private fun CoverStack(covers: List<String>) {
    Box(modifier = Modifier.width(92.dp).height(96.dp)) {
        if (covers.isEmpty()) {
            Box(
                Modifier.size(64.dp, 86.dp).align(Alignment.Center).clip(RoundedCornerShape(6.dp))
                    .background(ComicGlassBg).border(2.dp, ScrapbookDark, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) { Text("🗓️", fontSize = 26.sp) }
        }
        covers.forEachIndexed { i, url ->
            val angle = listOf(-10f, -3f, 4f, 10f)[i % 4]
            Box(
                modifier = Modifier.zIndex(i.toFloat())
                    .offset(x = (i * 9).dp, y = (i % 2 * 4).dp)
                    .dealIn(i * 90, i)
                    .rotate(angle)
                    .size(60.dp, 82.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(2.dp, ScrapbookDark, RoundedCornerShape(6.dp))
            ) {
                AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

// ─── Full sheet ───────────────────────────────────────────────────────────────

@Composable
fun WeeklyDigestHost() {
    if (!WeeklyDigestBus.open) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var digest by remember { mutableStateOf(WeeklyDigest.cached) }
    var refreshing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (digest == null) digest = WeeklyDigest.build() }
    val close = { WeeklyDigestBus.open = false }

    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(ComicGlassBg)) {
            HalftoneDots(modifier = Modifier.fillMaxSize(), color = CGreenDeep.copy(alpha = 0.07f))
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                val d = digest
                ComicPageHeader(
                    title = "WEEKLY UPDATE",
                    subtitle = if (d != null) "Week ${WeeklyDigest.weekNumber(d.weekStart)} · ${WeeklyDigest.rangeLabel(d.weekStart)}" else "Pulling this week…",
                    onBack = close,
                    showMenu = false,
                    marquee = "🗓️ THIS WEEK  •  NEW GAMES  •  DLC & EXPANSIONS  •  TOP STORIES  •  COMMUNITY  •  UPDATED WEEKLY",
                    trailing = {
                        ComicIconButton(Icons.Filled.Refresh, "Refresh") {
                            if (!refreshing) {
                                refreshing = true
                                Chiptune.play(Chiptune.Sfx.COIN)
                                scope.launch { digest = WeeklyDigest.build(force = true); refreshing = false }
                            }
                        }
                    }
                )
                when {
                    d == null || refreshing -> NowLoadingIndicator(label = "PULLING THIS WEEK")
                    d.isEmpty -> NoSaveDataState(subtitle = "Couldn't pull this week's updates. Check your connection and tap refresh.", title = "NO SIGNAL")
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (d.games.isNotEmpty()) {
                            item(key = "h_games") { DigestHeader("🎮 OUT THIS WEEK", d.games.size) }
                            val byDay = d.games.groupBy { dayLabel(it.releaseDate * 1000L) }
                            byDay.forEach { (day, games) ->
                                item(key = "day_$day") { DayChip(day) }
                                itemsIndexed(games, key = { _, g -> "g_${g.id}" }) { i, g ->
                                    Box(Modifier.dealIn(i.coerceAtMost(5) * 50, i)) { ReleaseRow(g) }
                                }
                            }
                        }
                        if (d.dlc.isNotEmpty()) {
                            item(key = "h_dlc") { DigestHeader("📦 DLC, EXPANSIONS & UPDATES", d.dlc.size) }
                            itemsIndexed(d.dlc, key = { _, g -> "d_${g.id}" }) { i, g ->
                                Box(Modifier.dealIn(i.coerceAtMost(5) * 50, i)) { ReleaseRow(g) }
                            }
                        }
                        if (d.news.isNotEmpty()) {
                            item(key = "h_news") { DigestHeader("📰 TOP STORIES", d.news.size) }
                            itemsIndexed(d.news, key = { _, a -> "n_${a.id}" }) { i, a ->
                                Box(Modifier.dealIn(i.coerceAtMost(5) * 50, i)) {
                                    StoryRow(a) {
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(a.sourceUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                        }
                                    }
                                }
                            }
                        }
                        if (d.community.isNotEmpty()) {
                            item(key = "h_comm") { DigestHeader("✍️ FROM THE COMMUNITY", d.community.size) }
                            itemsIndexed(d.community, key = { _, a -> "c_${a.id}" }) { i, a ->
                                Box(Modifier.dealIn(i.coerceAtMost(5) * 50, i)) {
                                    StoryRow(a) { close(); AppNavBus.go("ARTICLES") }
                                }
                            }
                        }
                        item(key = "cta") {
                            Spacer(Modifier.height(6.dp))
                            RetroGlassButton(text = "OPEN RELEASE CALENDAR", modifier = Modifier.fillMaxWidth(), onClick = {
                                close(); AppNavBus.go("EVENTS")
                            })
                            Spacer(Modifier.height(8.dp))
                            RetroGlassButton(text = "🔔 NOTIFICATION SETTINGS", modifier = Modifier.fillMaxWidth(), onClick = {
                                NotificationSettingsBus.open = true
                            })
                        }
                    }
                }
            }
        }
    }
}

private fun dayLabel(millis: Long): String {
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val diffDays = ((millis - todayStart) / 86_400_000L).toInt()
    val date = SimpleDateFormat("EEE MMM d", Locale.getDefault()).format(Date(millis)).uppercase()
    return when (diffDays) {
        0 -> "TODAY · $date"
        1 -> "TOMORROW · $date"
        else -> date
    }
}

@Composable
private fun DigestHeader(title: String, count: Int) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(5.dp).height(26.dp).clip(RoundedCornerShape(3.dp)).background(CGreen))
        Spacer(Modifier.width(8.dp))
        Text(title, modifier = Modifier.weight(1f).stampIn(title),
            style = TextStyle(fontFamily = BangersFontFamily, fontSize = 20.sp, letterSpacing = 1.sp, color = ScrapbookDark))
        Text("$count", modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(ScrapbookDark).padding(horizontal = 8.dp, vertical = 2.dp),
            style = TextStyle(fontFamily = BangersFontFamily, fontSize = 13.sp, color = CGreen))
    }
}

@Composable
private fun DayChip(day: String) {
    Text(day, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(CGreenDeep).padding(horizontal = 10.dp, vertical = 3.dp),
        style = TextStyle(fontFamily = BangersFontFamily, fontSize = 12.sp, letterSpacing = 1.sp, color = Color.White))
}

@Composable
private fun ReleaseRow(g: IGDBUpcomingGame) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White)
            .border(2.dp, ScrapbookDark, RoundedCornerShape(12.dp)).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(46.dp, 62.dp).clip(RoundedCornerShape(6.dp)).background(ComicGlassBg).border(1.5.dp, ScrapbookDark, RoundedCornerShape(6.dp))) {
            if (g.coverUrl != null) AsyncImage(model = g.coverUrl, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.halftoneReveal(g.coverUrl).fillMaxSize())
            else Text(g.name.take(1), modifier = Modifier.align(Alignment.Center), style = TextStyle(fontFamily = BangersFontFamily, fontSize = 22.sp, color = CGreenDeep))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(g.name, maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = TextStyle(fontFamily = BangersFontFamily, fontSize = 16.sp, lineHeight = 18.sp, color = ScrapbookDark))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(releaseTypeLabel(g.category, g.releaseDate),
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(CGreen).padding(horizontal = 5.dp, vertical = 1.dp),
                    style = TextStyle(fontFamily = BangersFontFamily, fontSize = 10.sp, color = Color.White))
                val plats = g.platforms.mapNotNull { platformAbbr(it) }.distinct().take(4).joinToString(" · ")
                if (plats.isNotBlank()) Text(plats, style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 11.sp, color = ScrapbookTextMuted))
            }
        }
        if (g.hypes > 0) {
            Text("🔥${g.hypes}", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 13.sp, color = CGreenDeep))
        }
    }
}

@Composable
private fun StoryRow(a: ArticleItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White)
            .border(2.dp, ScrapbookDark, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!a.imageUrl.isNullOrBlank()) {
            AsyncImage(model = a.imageUrl, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp, 48.dp).clip(RoundedCornerShape(6.dp)).border(1.5.dp, ScrapbookDark, RoundedCornerShape(6.dp)))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(a.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = TextStyle(fontFamily = BangersFontFamily, fontSize = 15.sp, lineHeight = 17.sp, color = ScrapbookDark))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                FreshBadge(a.publishedAt)
                Text("${a.sourceName.ifBlank { "@" + a.author }} · ${LiveFeeds.friendlyDate(a.publishedAt)}",
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 11.sp, color = ScrapbookTextMuted))
            }
        }
    }
}

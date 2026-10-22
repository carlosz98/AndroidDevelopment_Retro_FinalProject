package com.example.hubretro

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════════════════════════════
// Discover, upgraded (scrapbook style)
//   1. Jump chips pinned under the search bar (TODAY / TRENDING / PEOPLE / COLLECTIONS / WARP)
//   2. One search for players, games, albums, magazines and articles
//   3. "For you": your shelf (top games + soundtracks) decides what comes first
//   4. Album cards that play right away and can be saved
//   5. Pull to refresh + skeleton placeholders
//   6. "This week" banner
//   7. Top players with the reason they're trending
//   8. Events strip with countdowns
// ═══════════════════════════════════════════════════════════════════════════════

// ─── 4. Open a specific album in the Albums player ────────────────────────────

/** Discover sets this; AlbumsScreen opens that album's player as soon as it shows. */
object AlbumLaunch {
    var pendingAlbumId by mutableStateOf<String?>(null)
}

// ─── 3. For you ───────────────────────────────────────────────────────────────

data class ForYou(val keywords: Set<String>, val label: String) {
    val isEmpty get() = keywords.isEmpty()
}

private val platformAliases = listOf(
    listOf("playstation 2", "ps2") to "PS2",
    listOf("playstation 3", "ps3") to "PS3",
    listOf("playstation portable", "psp") to "PSP",
    listOf("playstation", "ps1", "psx") to "PS1",
    listOf("super nintendo", "snes", "super famicom") to "SNES",
    listOf("nintendo 64", "n64") to "N64",
    listOf("gamecube", "game cube") to "GAMECUBE",
    listOf("game boy advance", "gba") to "GBA",
    listOf("game boy", "gameboy") to "GAME BOY",
    listOf("nintendo entertainment system", "nes", "famicom") to "NES",
    listOf("genesis", "mega drive") to "GENESIS",
    listOf("dreamcast") to "DREAMCAST",
    listOf("saturn") to "SATURN",
    listOf("xbox") to "XBOX",
    listOf("arcade") to "ARCADE",
    listOf("pc (microsoft windows)", "windows", "pc", "dos") to "PC"
)

/** Builds the "for you" keywords from the games and soundtracks on your shelf. */
fun forYouFrom(profile: UserProfileData?): ForYou {
    if (profile == null) return ForYou(emptySet(), "")
    val words = mutableSetOf<String>()
    val platformsSeen = linkedSetOf<String>()
    profile.topGames.forEach { g ->
        val name = g["name"]?.toString().orEmpty()
        val platform = g["platform"]?.toString().orEmpty().lowercase()
        if (name.isNotBlank()) words += name.lowercase()
        platformAliases.firstOrNull { (aliases, _) -> aliases.any { platform.contains(it) } }?.let { (aliases, short) ->
            words += aliases; platformsSeen += short
        }
    }
    profile.topSoundtracks.forEach { s ->
        s["name"]?.toString()?.lowercase()?.takeIf { it.isNotBlank() }?.let { words += it }
        s["gameName"]?.toString()?.lowercase()?.takeIf { it.isNotBlank() }?.let { words += it }
    }
    // Split long titles into their strong words too ("chrono trigger" → "chrono")
    val extra = words.flatMap { it.split(' ', ':', '-') }.filter { it.length >= 5 }
    words += extra
    val label = platformsSeen.take(3).joinToString(" · ")
    return ForYou(words.filter { it.length >= 2 }.toSet(), label)
}

/** How well a piece of text matches your shelf. 0 = no match. */
fun ForYou.score(vararg texts: String?): Int {
    if (isEmpty) return 0
    val hay = texts.filterNotNull().joinToString(" ").lowercase()
    return keywords.count { k -> if (k.length <= 4) Regex("\\b${Regex.escape(k)}\\b").containsMatchIn(hay) else hay.contains(k) }
}

fun <T> List<T>.forYouFirst(fy: ForYou, text: (T) -> String): List<T> =
    if (fy.isEmpty) this else withIndex().sortedWith(compareByDescending<IndexedValue<T>> { fy.score(text(it.value)) }.thenBy { it.index }).map { it.value }

// ─── 1. Jump chips ────────────────────────────────────────────────────────────

val discoverGroups = listOf("TODAY", "TRENDING", "PEOPLE", "COLLECTIONS", "WARP ZONE")

@Composable
fun DiscoverJumpChips(active: String, onJump: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().background(ComicGlassBg).padding(vertical = 6.dp),
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(discoverGroups) { g ->
            val on = g == active
            Box(
                modifier = Modifier
                    .drawBehind {
                        if (on) drawRoundRect(ScrapbookDark, topLeft = Offset(2.dp.toPx(), 2.dp.toPx()), size = size, cornerRadius = CornerRadius(18.dp.toPx()))
                    }
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (on) CGreen else Color.White)
                    .border(2.dp, ScrapbookDark, RoundedCornerShape(18.dp))
                    .clickable { onJump(g) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(g, fontFamily = BangersFontFamily, color = if (on) ScrapbookDark else ScrapbookTextMuted, fontSize = 13.sp, letterSpacing = 0.5.sp)
            }
        }
    }
}

// ─── 2. Trending searches + unified results ───────────────────────────────────

@Composable
fun TrendingSearchChips(terms: List<String>, onClick: (String) -> Unit) {
    if (terms.isEmpty()) return
    Column(Modifier.fillMaxWidth().background(ComicGlassBg).padding(top = 2.dp, bottom = 4.dp)) {
        Text("🔥 TRENDING SEARCHES", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 12.sp, letterSpacing = 1.sp, modifier = Modifier.padding(start = 16.dp, bottom = 4.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(terms) { t ->
                Box(
                    Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White).border(1.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))
                        .clickable { onClick(t) }.padding(horizontal = 10.dp, vertical = 4.dp)
                ) { Text(t, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun DxResultHeader(title: String, count: Int) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f).height(2.dp).background(ScrapbookDark.copy(alpha = 0.15f)))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(CGreen).border(1.5.dp, ScrapbookDark, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 1.dp)) {
            Text("$count", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp)
        }
    }
}

@Composable
private fun DxResultRow(title: String, subtitle: String, imageUrl: String?, imageRes: Int? = null, fallback: String, badge: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .drawBehind { drawRoundRect(ScrapbookDark, topLeft = Offset(3.dp.toPx(), 3.dp.toPx()), size = size, cornerRadius = CornerRadius(12.dp.toPx())) }
            .clip(RoundedCornerShape(12.dp)).background(Color.White).border(2.dp, ScrapbookDark, RoundedCornerShape(12.dp))
            .clickable { onClick() }.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(ComicGlassBgAlt).border(1.5.dp, ScrapbookDark, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            when {
                imageRes != null -> androidx.compose.foundation.Image(painter = androidx.compose.ui.res.painterResource(imageRes), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                !imageUrl.isNullOrBlank() -> AsyncImage(model = imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else -> Text(fallback, fontSize = 22.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) Text(subtitle, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(ComicGlassBg).border(1.5.dp, ScrapbookDark, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
            Text(badge, fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 11.sp)
        }
    }
}

/** Every kind of result in one list, grouped. */
@Composable
fun DiscoverSearchResults(
    query: String,
    users: List<UserProfileData>,
    followingUids: Set<String>,
    currentUid: String?,
    games: List<IGDBGame>,
    isSearching: Boolean,
    albums: List<Album>,
    magazines: List<MagazineCover>,
    articles: List<ArticleItem>,
    onUser: (UserProfileData) -> Unit,
    onFollow: (UserProfileData) -> Unit,
    onGame: () -> Unit,
    onAlbum: (Album) -> Unit,
    onMagazine: () -> Unit,
    onArticle: () -> Unit
) {
    val total = users.size + games.size + albums.size + magazines.size + articles.size
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 32.dp)) {
        if (total == 0) {
            item {
                Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isSearching) "⏳" else "🔍", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(if (isSearching) "Searching…" else "No results for \"$query\"", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 14.sp)
                }
            }
            return@LazyColumn
        }
        if (users.isNotEmpty()) {
            item { DxResultHeader("PLAYERS", users.size) }
            items(users, key = { "u_${it.uid}" }) { user ->
                ScrapbookDiscoverUserCard(user = user, isFollowing = followingUids.contains(user.uid), isCurrentUser = user.uid == currentUid, onFollowClick = { onFollow(user) }, onTap = { onUser(user) })
            }
        }
        if (games.isNotEmpty()) {
            item { DxResultHeader("GAMES", games.size) }
            items(games, key = { "g_${it.id}" }) { g ->
                DxResultRow(g.name, listOfNotNull(g.platforms.firstOrNull(), g.releaseYear?.toString()).joinToString(" · "), g.coverUrl, fallback = "🎮", badge = "GAME", onClick = onGame)
            }
        }
        if (albums.isNotEmpty()) {
            item { DxResultHeader("ALBUMS", albums.size) }
            items(albums, key = { "a_${it.id}" }) { a ->
                DxResultRow(a.title, "${a.artist} · ${a.era}", a.coverImageUrl, imageRes = a.coverImageResId, fallback = "🎵", badge = "PLAY", onClick = { onAlbum(a) })
            }
        }
        if (magazines.isNotEmpty()) {
            item { DxResultHeader("MAGAZINES", magazines.size) }
            items(magazines, key = { "m_${it.id}" }) { m ->
                DxResultRow(m.title, m.platform, m.coverImageUrl, imageRes = m.coverImageResId, fallback = "📰", badge = "READ", onClick = onMagazine)
            }
        }
        if (articles.isNotEmpty()) {
            item { DxResultHeader("ARTICLES", articles.size) }
            items(articles, key = { "r_${it.id}" }) { r ->
                DxResultRow(r.title, r.author, null, fallback = "📝", badge = "READ", onClick = onArticle)
            }
        }
    }
}

// ─── 4. Album card that plays + saves ─────────────────────────────────────────

@Composable
fun DiscoverAlbumCard(album: Album, isSaved: Boolean, isPick: Boolean, onPlay: () -> Unit, onSave: () -> Unit) {
    val vColor = albumEraColor(album.era)
    val shape = RoundedCornerShape(14.dp)
    Box(modifier = Modifier.width(156.dp)) {
        Box(Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp).clip(shape).background(ScrapbookDark))
        Column(Modifier.fillMaxWidth().clip(shape).background(Color.White).border(2.5.dp, ScrapbookDark, shape).clickable { onPlay() }) {
            Box(Modifier.fillMaxWidth().height(132.dp).background(ComicGlassBgAlt).padding(10.dp)) {
                AlbumMiniVinyl(size = 100.dp, labelColor = vColor, modifier = Modifier.align(Alignment.CenterEnd).offset(x = 14.dp))
                Box(
                    Modifier.size(112.dp).align(Alignment.CenterStart)
                        .drawBehind { drawRoundRect(ScrapbookDark, topLeft = Offset(3.dp.toPx(), 3.dp.toPx()), size = size, cornerRadius = CornerRadius(10.dp.toPx())) }
                        .clip(RoundedCornerShape(10.dp)).border(2.dp, ScrapbookDark, RoundedCornerShape(10.dp))
                ) { AlbumCoverArt(album = album, modifier = Modifier.fillMaxSize(), initialsSize = 30) }
                if (isPick) {
                    Box(Modifier.align(Alignment.TopStart).offset(x = (-4).dp, y = (-4).dp).rotate(-4f).clip(RoundedCornerShape(6.dp)).background(CAcYellow)
                        .border(1.5.dp, ScrapbookDark, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("FOR YOU", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 10.sp)
                    }
                }
                // Save
                Box(
                    Modifier.align(Alignment.TopEnd).size(30.dp).clip(CircleShape).background(Color.White).border(2.dp, ScrapbookDark, CircleShape).clickable { onSave() },
                    contentAlignment = Alignment.Center
                ) { Icon(if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = if (isSaved) "Saved" else "Save", tint = if (isSaved) CGreenDeep else ScrapbookDark, modifier = Modifier.size(16.dp)) }
            }
            Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(album.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(album.artist, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("${album.era} · ${album.year ?: ""}", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp, maxLines = 1)
                        Text("▶ ${album.playCount} plays", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp, maxLines = 1)
                    }
                    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(CGreen).border(2.dp, ScrapbookDark, RoundedCornerShape(10.dp)).clickable { onPlay() }.padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text("▶ PLAY", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/** Album of the Day, pinned first in the albums row. */
@Composable
fun DiscoverAlbumOfTheDay(album: Album, isSaved: Boolean, onPlay: () -> Unit, onSave: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.matchParentSize().offset(x = 5.dp, y = 5.dp).clip(shape).background(ScrapbookDark))
        Row(
            Modifier.fillMaxWidth().clip(shape).background(Brush.horizontalGradient(listOf(CGreenMint, Color.White))).border(2.5.dp, ScrapbookDark, shape)
                .clickable { onPlay() }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.width(150.dp).height(110.dp)) {
                AlbumMiniVinyl(size = 100.dp, labelColor = albumEraColor(album.era), spinning = true, modifier = Modifier.align(Alignment.CenterEnd))
                Box(
                    Modifier.size(104.dp).align(Alignment.CenterStart)
                        .drawBehind { drawRoundRect(ScrapbookDark, topLeft = Offset(3.dp.toPx(), 3.dp.toPx()), size = size, cornerRadius = CornerRadius(10.dp.toPx())) }
                        .clip(RoundedCornerShape(10.dp)).border(2.dp, ScrapbookDark, RoundedCornerShape(10.dp))
                ) { AlbumCoverArt(album = album, modifier = Modifier.fillMaxSize(), initialsSize = 28) }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Box(Modifier.rotate(-3f).clip(RoundedCornerShape(6.dp)).background(ScrapbookDark).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("💿 ALBUM OF THE DAY", fontFamily = BangersFontFamily, color = CGreenMint, fontSize = 11.sp)
                }
                Spacer(Modifier.height(6.dp))
                Text(album.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
                Text(album.artist, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 12.sp, maxLines = 1)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(CGreen).border(2.dp, ScrapbookDark, RoundedCornerShape(10.dp)).clickable { onPlay() }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("▶ PLAY", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp)
                    }
                    Box(Modifier.size(30.dp).clip(CircleShape).background(Color.White).border(2.dp, ScrapbookDark, CircleShape).clickable { onSave() }, contentAlignment = Alignment.Center) {
                        Icon(if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = "Save", tint = if (isSaved) CGreenDeep else ScrapbookDark, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ─── 5. Skeletons ─────────────────────────────────────────────────────────────

@Composable
fun DiscoverHeroSkeleton() {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).border(2.5.dp, ScrapbookDark.copy(alpha = 0.25f), RoundedCornerShape(16.dp)).padding(12.dp)) {
        ShimmerCard(modifier = Modifier.fillMaxWidth().height(170.dp))
        Spacer(Modifier.height(10.dp))
        ShimmerCard(modifier = Modifier.fillMaxWidth(0.8f).height(18.dp))
        Spacer(Modifier.height(6.dp))
        ShimmerCard(modifier = Modifier.fillMaxWidth(0.5f).height(12.dp))
    }
}

// ─── 6. This week banner ──────────────────────────────────────────────────────

data class WeekSlide(val kicker: String, val title: String, val subtitle: String, val emoji: String, val imageUrl: String? = null, val onClick: () -> Unit = {})

@Composable
fun ThisWeekBanner(slides: List<WeekSlide>) {
    if (slides.isEmpty()) return
    var index by remember(slides.size) { mutableIntStateOf(0) }
    LaunchedEffect(slides.size) {
        while (true) { delay(4500); index = (index + 1) % slides.size }
    }
    val shape = RoundedCornerShape(18.dp)
    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.matchParentSize().offset(x = 5.dp, y = 5.dp).clip(shape).background(ScrapbookDark))
        Box(Modifier.fillMaxWidth().clip(shape).background(CGreen).border(3.dp, ScrapbookDark, shape)) {
            // halftone texture
            HalftoneDots(Modifier.matchParentSize(), spacing = 9.dp, dotRadius = 1.6.dp, color = ScrapbookDark.copy(alpha = 0.10f))
            AnimatedContent(
                targetState = index,
                transitionSpec = { (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut()) },
                label = "thisWeek"
            ) { i ->
                val s = slides[i.coerceIn(0, slides.lastIndex)]
                Row(Modifier.fillMaxWidth().clickable { s.onClick() }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(78.dp).drawBehind { drawRoundRect(ScrapbookDark, topLeft = Offset(3.dp.toPx(), 3.dp.toPx()), size = size, cornerRadius = CornerRadius(12.dp.toPx())) }
                            .clip(RoundedCornerShape(12.dp)).background(Color.White).border(2.dp, ScrapbookDark, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!s.imageUrl.isNullOrBlank()) AsyncImage(model = s.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else Text(s.emoji, fontSize = 36.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Box(Modifier.rotate(-2f).clip(RoundedCornerShape(6.dp)).background(ScrapbookDark).padding(horizontal = 7.dp, vertical = 2.dp)) {
                            Text(s.kicker, fontFamily = BangersFontFamily, color = CGreenMint, fontSize = 11.sp, letterSpacing = 0.5.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(s.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 21.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 22.sp)
                        Text(s.subtitle, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark.copy(alpha = 0.75f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            // dots
            Row(Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                slides.indices.forEach { d ->
                    Box(Modifier.size(if (d == index) 14.dp else 6.dp, 6.dp).clip(RoundedCornerShape(3.dp)).background(if (d == index) ScrapbookDark else ScrapbookDark.copy(alpha = 0.3f)))
                }
            }
        }
    }
}

// ─── 7. Top players + why they're trending ────────────────────────────────────

@Composable
fun TopPlayersWithReasons(
    users: List<UserProfileData>,
    weeklyPosts: Map<String, Int>,
    followingUids: Set<String>,
    currentUid: String,
    onFollow: (UserProfileData) -> Unit,
    onTap: (UserProfileData) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        users.forEachIndexed { rank, u ->
            val posts = weeklyPosts[u.uid] ?: 0
            val reason = when {
                posts >= 2 -> "🔥 $posts posts this week"
                posts == 1 -> "✍️ Posted this week"
                u.followersCount >= 10 -> "👥 ${formatCount(u.followersCount)} followers"
                u.topGames.isNotEmpty() -> "🎮 Into ${u.topGames.first()["name"]?.toString().orEmpty().take(22)}"
                else -> "⭐ Rising player"
            }
            Row(
                Modifier.fillMaxWidth()
                    .drawBehind { drawRoundRect(ScrapbookDark, topLeft = Offset(3.dp.toPx(), 3.dp.toPx()), size = size, cornerRadius = CornerRadius(14.dp.toPx())) }
                    .clip(RoundedCornerShape(14.dp)).background(Color.White).border(2.dp, ScrapbookDark, RoundedCornerShape(14.dp))
                    .clickable { onTap(u) }.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#${rank + 1}", fontFamily = BangersFontFamily, color = if (rank == 0) CGreenDeep else ScrapbookTextMuted, fontSize = 20.sp, modifier = Modifier.width(34.dp))
                Box(Modifier.size(44.dp).clip(CircleShape).background(ComicGlassBgAlt).border(2.dp, ScrapbookDark, CircleShape), contentAlignment = Alignment.Center) {
                    if (u.profilePictureUrl.isNotBlank()) AsyncImage(model = u.profilePictureUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else Text(u.username.take(1).uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(u.username.uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Box(Modifier.padding(top = 3.dp).clip(RoundedCornerShape(8.dp)).background(ComicGlassBg).border(1.dp, ScrapbookDark.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 1.dp)) {
                        Text(reason, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = CGreenDeep, fontSize = 11.sp, maxLines = 1)
                    }
                }
                if (u.uid != currentUid) {
                    val following = u.uid in followingUids
                    Box(
                        Modifier.clip(RoundedCornerShape(10.dp)).background(if (following) Color.White else CGreen).border(2.dp, ScrapbookDark, RoundedCornerShape(10.dp))
                            .clickable { onFollow(u) }.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) { Text(if (following) "FOLLOWING" else "FOLLOW", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp) }
                }
            }
        }
    }
}

// ─── 8. Events strip ──────────────────────────────────────────────────────────

data class StripEvent(val title: String, val whenMs: Long, val emoji: String, val kind: String, val imageUrl: String? = null)

@Composable
fun EventsStrip(events: List<StripEvent>, onOpen: () -> Unit) {
    if (events.isEmpty()) return
    // Re-render countdowns once a minute
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(60_000); now = System.currentTimeMillis() } }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(start = 2.dp, end = 8.dp, top = 2.dp, bottom = 6.dp)) {
        itemsIndexed(events) { _, e ->
            val left = e.whenMs - now
            val countdown = when {
                left <= 0 -> "HAPPENING NOW"
                left < 3_600_000L -> "IN ${left / 60_000L} MIN"
                left < 86_400_000L -> "IN ${left / 3_600_000L}H ${(left % 3_600_000L) / 60_000L}M"
                else -> "IN ${left / 86_400_000L}D ${(left % 86_400_000L) / 3_600_000L}H"
            }
            val soon = left < 86_400_000L
            Column(
                Modifier.width(170.dp)
                    .drawBehind { drawRoundRect(ScrapbookDark, topLeft = Offset(3.dp.toPx(), 3.dp.toPx()), size = size, cornerRadius = CornerRadius(14.dp.toPx())) }
                    .clip(RoundedCornerShape(14.dp)).background(Color.White).border(2.dp, ScrapbookDark, RoundedCornerShape(14.dp))
                    .clickable { onOpen() }
            ) {
                Box(Modifier.fillMaxWidth().background(if (soon) CGreen else ComicGlassBgAlt).padding(horizontal = 10.dp, vertical = 5.dp)) {
                    Text("⏱ $countdown", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, letterSpacing = 0.5.sp)
                }
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(ComicGlassBg).border(1.5.dp, ScrapbookDark, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        if (!e.imageUrl.isNullOrBlank()) AsyncImage(model = e.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else Text(e.emoji, fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 15.sp)
                        Text(e.kind, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

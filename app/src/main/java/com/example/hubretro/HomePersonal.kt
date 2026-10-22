package com.example.hubretro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════════════
// Home = YOU and YOUR PEOPLE. Discover = the whole retro world.
// These sections are the personal parts of Home:
//   • HomePlayerCard   — your level, XP bar and badges
//   • HomeShelfSection — your top games and soundtracks from profile setup
//   • HomeCrewSection  — the people you follow
//   • HomeDiscoverTeaser — one doorway to Discover instead of copying its sections
// ═══════════════════════════════════════════════════════════════════════════════

/** Scrapbook card: white, ink border, hard offset shadow (same look as the rest of the app). */
@Composable
private fun HomeInkCard(
    modifier: Modifier = Modifier,
    radius: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    Box(modifier = modifier) {
        Box(Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp).clip(shape).background(ScrapbookDark))
        Column(
            modifier = Modifier.fillMaxWidth().clip(shape).background(Color.White)
                .border(2.dp, ScrapbookDark, shape)
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun HomeSectionTitle(title: String, emoji: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.width(5.dp).height(26.dp).clip(RoundedCornerShape(3.dp)).background(CGreen))
            Text("$emoji $title", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.sp)
        }
        if (action != null) {
            Text(action, fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 14.sp, modifier = Modifier.clickable { onAction() })
        }
    }
}

private fun fixImageUrl(url: String): String = when {
    url.startsWith("//") -> "https:$url"
    else -> url
}

// ─── Player card ──────────────────────────────────────────────────────────────

@Composable
fun HomePlayerCard(
    profile: UserProfileData?,
    achievements: AchievementsState,
    onOpenProfile: () -> Unit
) {
    val xp = achievements.xp
    val level = getRetroLevel(xp)
    val earned = achievements.badges.count { it.isEarned }
    val name = profile?.username?.ifBlank { null } ?: "Player"

    HomeInkCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), onClick = onOpenProfile) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(60.dp).softWell(radius = 30.dp, inkBorder = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                val pic = profile?.profilePictureUrl.orEmpty()
                if (pic.isNotBlank()) {
                    AsyncImage(model = pic, contentDescription = name, contentScale = ContentScale.Crop, modifier = Modifier.size(46.dp).clip(CircleShape).border(2.dp, CGreen, CircleShape))
                } else {
                    Text(name.take(1).uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 26.sp)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("WELCOME BACK,", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 12.sp)
                Text(name.uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(14.dp))
        // Same XP bar as the profile page (level title, XP stamp, animated fill, next-level line).
        // It brings its own 16dp side padding, so pull it back out to the card's edges.
        Box(Modifier.layout { measurable, constraints ->
            val extra = 16.dp.roundToPx() * 2
            val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + extra, minWidth = (constraints.minWidth + extra).coerceAtMost(constraints.maxWidth + extra)))
            layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
        }) {
            ScrapbookXPProgressBar(xp = xp, level = level, progress = getLevelProgress(xp), xpToNext = getXpToNextLevel(xp))
        }
        Spacer(Modifier.height(14.dp))
        // Same stat cards as the profile page
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(start = 2.dp, end = 6.dp, top = 2.dp, bottom = 6.dp)) {
            item { AnimatedStatCard(formatCount(profile?.followersCount ?: 0), "FOLLOWERS", 0.6f) { onOpenProfile() } }
            item { AnimatedStatCard(formatCount(profile?.followingCount ?: 0), "FOLLOWING", 0.6f) { onOpenProfile() } }
            item { AnimatedStatCard("$earned", "BADGES", 0.6f) { onOpenProfile() } }
            item { AnimatedStatCard("$xp", "TOTAL XP", 0.6f) { onOpenProfile() } }
        }
    }
}

@Composable
private fun HomeStatPill(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.softWell(radius = 12.dp).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$emoji $value", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp)
        Text(label, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 10.sp)
    }
}

// ─── Your shelf ───────────────────────────────────────────────────────────────

private data class ShelfItem(val title: String, val subtitle: String, val coverUrl: String, val isMusic: Boolean)

@Composable
fun HomeShelfSection(
    profile: UserProfileData?,
    onOpenGames: () -> Unit,
    onOpenAlbums: () -> Unit,
    onEditFavorites: () -> Unit
) {
    val items = buildList {
        profile?.topGames.orEmpty().forEach { g ->
            add(ShelfItem(
                title = g["name"]?.toString().orEmpty(),
                subtitle = g["platform"]?.toString().orEmpty().ifBlank { g["releaseYear"]?.toString().orEmpty() },
                coverUrl = fixImageUrl(g["coverUrl"]?.toString().orEmpty()),
                isMusic = false
            ))
        }
        profile?.topSoundtracks.orEmpty().forEach { s ->
            add(ShelfItem(
                title = s["name"]?.toString().orEmpty(),
                subtitle = s["gameName"]?.toString().orEmpty(),
                coverUrl = fixImageUrl(s["coverUrl"]?.toString().orEmpty()),
                isMusic = true
            ))
        }
    }.filter { it.title.isNotBlank() }

    Column(Modifier.fillMaxWidth()) {
        HomeSectionTitle("YOUR SHELF", "🎒", action = if (items.isEmpty()) null else "EDIT →", onAction = onEditFavorites)
        Spacer(Modifier.height(12.dp))
        if (items.isEmpty()) {
            HomeInkCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), onClick = onEditFavorites) {
                Text("Your shelf is empty", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp)
                Text("Add your favorite games and soundtracks on your profile and they'll live here.", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
            }
        } else {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(items) { item ->
                    Column(
                        modifier = Modifier.width(110.dp).clickable { if (item.isMusic) onOpenAlbums() else onOpenGames() }
                    ) {
                        Box {
                            Box(Modifier.size(110.dp).offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(12.dp)).background(ScrapbookDark))
                            Box(
                                Modifier.size(110.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).border(2.dp, ScrapbookDark, RoundedCornerShape(12.dp)).padding(5.dp)
                                    .softWell(radius = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.coverUrl.isNotBlank()) {
                                    AsyncImage(model = item.coverUrl, contentDescription = item.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)))
                                } else {
                                    Text(if (item.isMusic) "🎵" else "🎮", fontSize = 34.sp)
                                }
                                Box(
                                    Modifier.align(Alignment.TopStart).padding(5.dp).inkPop(radius = 6.dp, fill = if (item.isMusic) CGreen else Color.White, shadow = 1.5.dp, border = 1.dp).padding(horizontal = 5.dp, vertical = 1.dp)
                                ) { Text(if (item.isMusic) "OST" else "GAME", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 10.sp) }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(item.title, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (item.subtitle.isNotBlank()) {
                            Text(item.subtitle, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

// ─── Your crew ────────────────────────────────────────────────────────────────

@Composable
fun HomeCrewSection(crew: List<UserProfileData>, onFindPeople: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        HomeSectionTitle("YOUR CREW", "👥", action = "FIND MORE →", onAction = onFindPeople)
        Spacer(Modifier.height(12.dp))
        if (crew.isEmpty()) {
            HomeInkCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), onClick = onFindPeople) {
                Text("No crew yet", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp)
                Text("Follow other players in Discover and they'll show up here.", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
            }
        } else {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(crew, key = { it.uid }) { user -> PolaroidUserCard(user = user, onTap = onFindPeople) }
            }
        }
    }
}

// ─── Doorway to Discover ──────────────────────────────────────────────────────

@Composable
fun HomeDiscoverTeaser(onOpenDiscover: () -> Unit) {
    HomeInkCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), onClick = onOpenDiscover) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🌀", fontSize = 34.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("EXPLORE THE RETRO WORLD", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp)
                Text("News, deals, game of the day, live streams, albums, magazines and new players.", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.inkPop(radius = 20.dp, fill = CGreen, shadow = 2.dp, border = 2.dp).padding(horizontal = 12.dp, vertical = 6.dp)
            ) { Text("GO →", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp) }
        }
    }
}

package com.example.hubretro

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

// ─── Data Models ──────────────────────────────────────────────────────────────

data class NewsItem(
    val title: String,
    val description: String,
    val imageUrl: String?,
    val url: String,
    val source: String,
    val sourceColor: Color,
    val publishedAt: String
)

data class GameDeal(
    val title: String,
    val salePrice: String,
    val normalPrice: String,
    val savings: Int,
    val thumb: String,
    val storeId: String
)

data class RetroGameOfDay(
    val id: Int,
    val name: String,
    val coverUrl: String?,
    val summary: String?,
    val rating: Double?,
    val releaseYear: Int?
)

data class DiscoverResult(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: DiscoverCategory
)

enum class DiscoverCategory(val label: String, val color: Color) {
    USER("USER", Color.White),
    MAGAZINE("MAGAZINE", Color.White),
    ALBUM("ALBUM", Color.White),
    ARTICLE("ARTICLE", Color.White)
}

val discoverFilters = listOf("ALL", "PEOPLE", "ARTICLES", "MAGAZINES", "ALBUMS", "LIVE")

// ─── RSS Sources ──────────────────────────────────────────────────────────────

val rssSources = listOf(
    Triple("IGN",       CAcRed, "https://feeds.feedburner.com/ign/games-all"),
    Triple("Kotaku",    CGreen, "https://kotaku.com/rss"),
    Triple("Eurogamer", CAcBlue, "https://www.eurogamer.net/?format=rss")
)

val newsPageSize = 10

val retroGameIds = listOf(
    1020, 1942, 768, 1877, 472, 119, 324, 2131,
    282, 510, 481, 1030, 1941, 542, 1746, 311,
    1074, 236, 533, 1069
)

// ─── Network Helpers ──────────────────────────────────────────────────────────

suspend fun fetchNewsFromRss(
    sourceName: String,
    sourceColor: Color,
    url: String,
    limit: Int = 8
): List<NewsItem> = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).addHeader("User-Agent", "Mozilla/5.0").build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext emptyList()
        val items = mutableListOf<NewsItem>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(StringReader(body))
        var currentTitle = ""; var currentDesc = ""; var currentLink = ""
        var currentPubDate = ""; var currentImage: String? = null
        var insideItem = false; var currentTag = ""
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT && items.size < limit) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name ?: ""
                    if (currentTag == "item") { insideItem = true; currentTitle = ""; currentDesc = ""; currentLink = ""; currentPubDate = ""; currentImage = null }
                    if (insideItem) {
                        when (currentTag) {
                            "content", "thumbnail" -> { val imgUrl = parser.getAttributeValue(null, "url"); if (!imgUrl.isNullOrBlank() && currentImage == null) currentImage = imgUrl }
                            "enclosure" -> { val encType = parser.getAttributeValue(null, "type") ?: ""; if (encType.startsWith("image")) { val imgUrl = parser.getAttributeValue(null, "url"); if (!imgUrl.isNullOrBlank()) currentImage = imgUrl } }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideItem) {
                        when (currentTag) {
                            "title" -> currentTitle += parser.text ?: ""
                            "description" -> { val raw = parser.text ?: ""; if (currentImage == null) { Regex("""<img[^>]+src=["']([^"']+)["']""").find(raw)?.groupValues?.getOrNull(1)?.let { currentImage = it } }; currentDesc += raw.replace(Regex("<[^>]*>"), "").trim() }
                            "link" -> currentLink += parser.text ?: ""
                            "pubDate" -> currentPubDate += parser.text ?: ""
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item" && insideItem) {
                        if (currentTitle.isNotBlank() && currentLink.isNotBlank()) {
                            items.add(NewsItem(title = currentTitle.trim(), description = currentDesc.trim().take(200), imageUrl = currentImage, url = currentLink.trim(), source = sourceName, sourceColor = sourceColor, publishedAt = formatRssDate(currentPubDate.trim())))
                        }
                        insideItem = false
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }
        items
    } catch (e: Exception) { emptyList() }
}

suspend fun fetchArticleContent(url: String): String = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).addHeader("User-Agent", "Mozilla/5.0 (Android)").build()
        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: return@withContext ""
        var text = html
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<nav[^>]*>[\\s\\S]*?</nav>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<header[^>]*>[\\s\\S]*?</header>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<footer[^>]*>[\\s\\S]*?</footer>", RegexOption.IGNORE_CASE), "")
        val paragraphs = Regex("<p[^>]*>([\\s\\S]*?)</p>", RegexOption.IGNORE_CASE)
            .findAll(text).map { it.groupValues[1].replace(Regex("<[^>]*>"), "").trim() }
            .filter { it.length > 50 }.take(20).joinToString("\n\n")
        paragraphs.ifBlank { text.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim().take(3000) }
    } catch (e: Exception) { "" }
}

suspend fun fetchGameDeals(): List<GameDeal> = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()
        val request = Request.Builder().url("https://www.cheapshark.com/api/1.0/deals?sortBy=Deal&onSale=1&upperPrice=20&pageSize=8").build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext emptyList()
        val arr = JSONArray(body)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            GameDeal(title = obj.optString("title"), salePrice = obj.optString("salePrice"), normalPrice = obj.optString("normalPrice"), savings = obj.optDouble("savings", 0.0).toInt(), thumb = obj.optString("thumb"), storeId = obj.optString("storeID"))
        }.filter { it.title.isNotBlank() }
    } catch (e: Exception) { emptyList() }
}

suspend fun fetchRetroGameOfDay(): RetroGameOfDay? {
    return try { IGDBRepository.fetchGameById(retroGameIds.random()) } catch (e: Exception) { null }
}

fun formatRssDate(raw: String): String {
    return try { val parts = raw.split(" "); if (parts.size >= 4) "${parts[1]} ${parts[2]} ${parts[3]}" else raw } catch (e: Exception) { raw }
}

fun storeName(storeId: String) = when (storeId) {
    "1" -> "Steam"; "2" -> "GamersGate"; "3" -> "GreenManGaming"
    "7" -> "GOG"; "8" -> "Origin"; "11" -> "Humble"
    "13" -> "Uplay"; "15" -> "Fanatical"; "25" -> "Epic"
    else -> "Store"
}

// ─── New Feature Data ─────────────────────────────────────────────────────────

data class GamingHistoryEvent(
    val year: Int,
    val month: Int,
    val day: Int,
    val title: String,
    val description: String,
    val console: String,
    val emoji: String
)

data class CommunityPulseItem(
    val topic: String,
    val emoji: String,
    val newPosts: Int,
    val minutesAgo: Int,
    val color: Color
)

data class TrendingTag(
    val tag: String,
    val context: String,
    val growth: String,
    val count: Int,
    val isHot: Boolean = true
)

val discoverGamingQuotes = listOf(
    "It's dangerous to go alone! Take this." to "The Legend of Zelda (1986)",
    "The right man in the wrong place can make all the difference in the world." to "Half-Life 2 (2004)",
    "A hero need not speak. When he is gone, the world will speak for him." to "Halo",
    "The cake is a lie." to "Portal (2007)",
    "It's-a me, Mario!" to "Super Mario 64 (1996)",
    "War. War never changes." to "Fallout (1997)",
    "Stay a while and listen!" to "Diablo II (2000)",
    "Do a barrel roll!" to "Star Fox 64 (1997)",
    "Thank you Mario! But our Princess is in another castle." to "Super Mario Bros (1985)",
    "Get over here!" to "Mortal Kombat (1992)",
    "You've met with a terrible fate, haven't you?" to "Majora's Mask (2000)",
    "The price of freedom is eternal vigilance." to "Bioshock Infinite (2013)",
    "What is a man? A miserable little pile of secrets!" to "Castlevania: SotN (1997)"
)

val gamingHistoryEvents = listOf(
    GamingHistoryEvent(1985, 7, 15, "Super Mario Bros. Debuts", "Nintendo releases what will become the best-selling NES game of all time, shipping with every new Famicom.", "NES", "🍄"),
    GamingHistoryEvent(1989, 7, 31, "Game Boy Launches in North America", "Nintendo's portable powerhouse arrives in the US with Tetris bundled in every box — a masterstroke of game marketing.", "GB", "🎮"),
    GamingHistoryEvent(1997, 7, 9, "Final Fantasy VII Ships to NA", "Square's JRPG epic arrives on PS1 and blows Western minds with its cinematic storytelling.", "PS1", "🗡️"),
    GamingHistoryEvent(1996, 7, 21, "Crash Bandicoot Releases", "Naughty Dog's marsupial platformer becomes a PS1 icon, selling 6.8M copies worldwide.", "PS1", "🦊"),
    GamingHistoryEvent(1993, 7, 29, "Mortal Kombat Arrives on Home Consoles", "The controversial fighter launches on SNES and Genesis simultaneously — with blood code on Genesis.", "SNES", "💀"),
    GamingHistoryEvent(2001, 7, 19, "GameCube Launches in Japan", "Nintendo's purple lunchbox arrives at ¥25,000 bundled with Wave Race: Blue Storm.", "GCN", "💿"),
    GamingHistoryEvent(1991, 7, 13, "Sonic Hits Europe", "Sega's blue blur arrives across the Atlantic, bundled with the Mega Drive and taking on Mario at his own game.", "SEGA", "💨"),
    GamingHistoryEvent(1983, 7, 15, "Famicom Launches in Japan", "Nintendo's Family Computer goes on sale for ¥14,800, igniting gaming's golden age after the 1983 crash.", "NES", "🏯"),
    GamingHistoryEvent(1999, 7, 8, "Sega Dreamcast Launches in North America", "Sega's final console arrives with 18 launch titles and the first console web browser.", "DC", "🌀"),
    GamingHistoryEvent(2003, 7, 27, "Pokémon Ruby & Sapphire Reach 1M US Sales", "The Hoenn region games dominate the GBA just weeks after their North American debut.", "GBA", "🔴"),
    GamingHistoryEvent(1987, 7, 1, "Mega Man Released in Japan", "Capcom launches its iconic robot-hero series on Famicom with a bold blue palette that defined an era.", "NES", "🤖"),
    GamingHistoryEvent(2005, 7, 15, "Guitar Hero Announced", "Harmonix reveals the rhythm game that will define an era, leading to $1B+ in yearly franchise sales.", "PS2", "🎸"),
    GamingHistoryEvent(1998, 4, 4, "Baldur's Gate Enters Gold", "BioWare's RPG epic wraps development ahead of its landmark August release.", "PC", "🐉"),
    GamingHistoryEvent(1994, 5, 23, "Donkey Kong Country Previewed at CES", "Rare stuns the industry with pre-rendered 3D graphics no one thought possible on a 16-bit console.", "SNES", "🦍"),
    GamingHistoryEvent(1998, 11, 21, "The Legend of Zelda: Ocarina of Time Ships", "The game that would define 3D adventure gaming for a generation arrives on N64.", "N64", "🎵")
)

val communityPulseItems = listOf(
    CommunityPulseItem("Final Fantasy", "🗡️", 127, 3, CAcPurple),
    CommunityPulseItem("Retro Speedruns", "⚡", 89, 8, CAcYellow),
    CommunityPulseItem("Nintendo Memories", "🍄", 214, 1, CAcRed),
    CommunityPulseItem("PS1 Era Vibes", "💿", 66, 15, CAcBlue),
    CommunityPulseItem("SEGA Dreamcast", "🌀", 43, 22, CAcBlue),
    CommunityPulseItem("Pixel Art Showcase", "🖼️", 158, 5, CGreenDeep)
)

val hotTrendingTags = listOf(
    TrendingTag("#RetroRPG", "Final Fantasy anniversary · fan art surge", "↑ 3× this week", 1847, true),
    TrendingTag("#PixelPerfect", "Trending among artists and speedrunners", "↑ 89% today", 932, true),
    TrendingTag("#PSOneMemories", "PS1's 25th anniversary in NA", "↑ 2× this week", 2341, true),
    TrendingTag("#NESClassics", "New community challenge started", "↑ 44% this week", 678, true),
    TrendingTag("#RetroHorror", "October vibes hitting early", "↑ 120% spike", 445, true),
    TrendingTag("#SonicUnderground", "Old community revived by new members", "↑ 5× this week", 312, true)
)

val risingTrendingTags = listOf(
    TrendingTag("#TurboGrafx16", "Small but passionate community growing fast", "↑ 8× this month", 187, false),
    TrendingTag("#MegaCD", "Sega's underrated CD add-on getting attention", "↑ 6× this week", 134, false),
    TrendingTag("#VirtualBoy", "Nostalgia posts picking up steam after documentary", "↑ 4× this week", 98, false),
    TrendingTag("#Atari2600", "New streaming series driving interest", "↑ 3× today", 267, false),
    TrendingTag("#RetroChillZone", "Ambient gaming streams taking off", "↑ 350% this week", 445, false),
    TrendingTag("#GameBoyCamera", "Photo filter trend using GB Camera aesthetic", "↑ 10× this week", 712, false)
)

val sampleFallbackArticles = listOf(
    ArticleItem(id = "fa1", title = "The Golden Age of JRPGs: Why 90s RPGs Still Hold Up", snippet = "From Final Fantasy VI to Chrono Trigger, we revisit the decade that defined role-playing games forever.", fullContent = "The 1990s were a golden era for Japanese role-playing games. With limited hardware pushing developers to pour creativity into storytelling and music rather than graphics, we got some of the most emotionally resonant games ever made.\n\n## Final Fantasy VI — The Crown Jewel\nSquare's masterpiece pushed the SNES to its limits with an ensemble cast, a haunting villain, and a world that actually ended at the midpoint. No other game had dared that before.\n\n## Chrono Trigger — Still Perfect\nDesigned by the 'Dream Team' of Sakaguchi, Toriyama, and Horii, Chrono Trigger remains the gold standard for time-travel storytelling. Fifteen different endings. A New Game+ mode. A battle system so clean it's still copied today.", author = "RetroHub Staff", category = "RETRO", likeCount = 42, viewCount = 380, readingTimeMinutes = 4, date = "Jul 10, 2026"),
    ArticleItem(id = "fa2", title = "Top 10 Hidden Gems on the SNES You Probably Missed", snippet = "Beyond the blockbusters, the Super Nintendo had a library of incredible games that flew under the radar.", fullContent = "Everyone knows Super Mario World and A Link to the Past. But the SNES had hundreds of titles — some of its best were easy to miss.\n\n## ActRaiser — Half God, Half City Builder\nYou play as God. You shoot demons in sidescrolling segments, then guide your people as they settle new lands. Nothing else was like it.\n\n## Terranigma — The Europe-Only Masterpiece\nNever officially released in North America, Terranigma tells the story of a boy who literally resurrects the continents. It's one of the most ambitious RPGs ever made, and most Western players only discovered it via fan translation.", author = "VintageVault", category = "RETRO", likeCount = 28, viewCount = 210, readingTimeMinutes = 3, date = "Jul 8, 2026"),
    ArticleItem(id = "fa3", title = "Sonic vs Mario: 30 Years of Gaming's Greatest Rivalry", snippet = "How two mascots defined console wars, childhood arguments, and gaming history across three decades.", fullContent = "It began in 1991 when Sega positioned Sonic as the fast alternative to Nintendo's plumber. What followed was a decade-long war for living rooms, playgrounds, and magazine cover stories.\n\n## The Console Wars Were Real\nPrimary school playgrounds were divided. You either had a Mega Drive or a SNES — and you defended that choice with your life. Marketing teams leaned in hard: 'Blast Processing' vs Nintendo's seal of quality.\n\n## Where Are They Now?\nMario has maintained unprecedented consistency. Sonic has had a rockier road — but the recent films and Sonic Frontiers show there's still love for the blue blur.", author = "RetroRival", category = "CULTURE", likeCount = 61, viewCount = 520, readingTimeMinutes = 4, date = "Jul 5, 2026"),
    ArticleItem(id = "fa4", title = "Why PS1-Era Graphics Hit Different in 2025", snippet = "Wobbly polygons, pre-rendered backgrounds, early 3D — why does that era feel so special now?", fullContent = "There's something deeply comforting about firing up a PS1 emulator. The jagged polygons, limited draw distance, music that loops every 90 seconds — it all transports you back to an era when 3D was magic.\n\n## The Jank Was the Feature\nPS1 hardware couldn't afford z-buffering, so polygons visibly warped as you moved. That wobble, to a generation of players, is nostalgia rendered in silicon.\n\n## Pre-Rendered Backgrounds Were Art\nFinal Fantasy VII's Midgar, Resident Evil's Spencer Mansion — these were painstakingly painted environments that looked far beyond anything real-time 3D could do at the time.", author = "PolyNostalgia", category = "OPINION", likeCount = 35, viewCount = 290, readingTimeMinutes = 3, date = "Jul 3, 2026"),
    ArticleItem(id = "fa5", title = "The Game Boy's Unbreakable Legacy: 35 Years Later", snippet = "Nintendo's handheld brick outlived its competitors, sold 118 million units, and shaped gaming culture for a generation.", fullContent = "The original Game Boy was not the most powerful handheld. The Sega Game Gear had a color screen. The Atari Lynx had a backlit display. But the Game Boy had Tetris — and that was enough.\n\n## Designed to Survive Anything\nA Game Boy survived a bombing in the Gulf War and still played Tetris. Nintendo literally put it on display in their museum. The device's build quality was a statement: this is for real life, not just living rooms.\n\n## The Link Cable Changed Everything\nMultiplayer gaming on the go. Two kids. Two Game Boys. One Link Cable. Pokémon Red and Blue built an entire social ecosystem around that cable.", author = "HandheldHistorian", category = "RETRO", likeCount = 49, viewCount = 410, readingTimeMinutes = 4, date = "Jun 28, 2026"),
    ArticleItem(id = "fa6", title = "Speedrunning Retro Games: The Community That Won't Let Go", snippet = "How any% runs, glitch exploitation, and a passionate community keep decades-old games feeling brand new.", fullContent = "It's 2 AM somewhere in Europe. A runner has been staring at the same 8-bit dungeon for six hours — not to beat the game, but to beat themselves. This is speedrunning.\n\n## Why Old Games?\nRetro games tend to be short, deterministic, and full of exploitable quirks baked into the hardware. The 'wrong warp' in Super Mario Bros., the 'wrong warp' in Zelda — these are almost gifts from the developers.\n\n## SGDQ Changed Everything\nSummer Games Done Quick turned speedrunning into a mainstream phenomenon. Millions of viewers, millions in charity donations, and a new generation of fans discovering 30-year-old games.", author = "SpeedScene", category = "CULTURE", likeCount = 33, viewCount = 275, readingTimeMinutes = 3, date = "Jun 22, 2026"),
    ArticleItem(id = "fa7", title = "Koji Kondo: The Man Who Scored Our Childhood", snippet = "From Super Mario Bros. to Ocarina of Time, how one composer defined what video game music could be.", fullContent = "In 1985, a 23-year-old Koji Kondo composed the Super Mario Bros. theme in a single afternoon. Over 35 years later, it remains one of the most recognized melodies on Earth.\n\n## The Legend of Zelda — Music as World-Building\nThe Overworld Theme, Zelda's Lullaby, the dungeon music — Kondo didn't just score a game, he built an entire sonic world that players could navigate by ear alone.\n\n## Ocarina of Time — A New Language\nThe Ocarina introduced music as a game mechanic. Twelve songs. Each one a memory. It was a design achievement that's never been replicated quite the same way.", author = "SoundByte", category = "OST", likeCount = 57, viewCount = 480, readingTimeMinutes = 4, date = "Jun 18, 2026"),
    ArticleItem(id = "fa8", title = "The Famicom's Secret: Why NES Games Were So Hard", snippet = "Japanese import, western port — the surprising reason American NES games were brutally harder than their Japanese counterparts.", fullContent = "Ask anyone who grew up with an NES and they'll tell you: those games were punishing. Ninja Gaiden. Battletoads. Ghosts 'n Goblins.\n\n## Nintendo of America's Content Guidelines\nNOA required localised games to remove any references to religion, alcohol, or sexual content — but also quietly encouraged difficulty increases to extend rental appeal. If a game took 45 minutes to complete, it wouldn't sell.\n\n## The Hidden Easy Modes\nMany Famicom originals had difficulty select screens that were quietly removed. Gradius had a Konami Code that gave you everything. Contra — 30 lives. These weren't cheats; they were the original difficulty settings.", author = "DifficultySpike", category = "GAMING", likeCount = 44, viewCount = 350, readingTimeMinutes = 5, date = "Jun 15, 2026"),
    ArticleItem(id = "fa9", title = "Pixel Art in 2025: From Limitation to Artform", snippet = "Once a technical constraint, pixel art is now a deliberate aesthetic choice embraced by indie studios worldwide.", fullContent = "When 8-bit developers placed pixels on screen, they weren't making an artistic choice — they were working within the only tools they had. Today's pixel artists choose those constraints deliberately.\n\n## Celeste, Shovel Knight, and the Revival\nThe indie renaissance of the 2010s brought pixel art back with a vengeance. But these weren't imitations — they were refinements. Cleaner palettes, smoother animations, expressive character designs.\n\n## The Palette Constraint Is the Point\nLimiting yourself to 16 or 32 colours forces decisions. Every pixel has weight. Modern pixel art communities hold 'palette jams' where all entrants must use the same 8 colours.", author = "PixelPioneer", category = "PIXEL ART", likeCount = 38, viewCount = 315, readingTimeMinutes = 3, date = "Jun 10, 2026"),
    ArticleItem(id = "fa10", title = "Review: Chrono Trigger (SNES) — Still a Perfect 10", snippet = "We replay Squaresoft's 1995 masterpiece and ask: does it hold up? Spoiler — it does, completely.", fullContent = "Some games age. Chrono Trigger doesn't.\n\n## The Combat Is Still Perfect\nActive Time Battle with no random encounters — enemies are visible on screen. You choose when to fight. The dual and triple techs make every party combination feel worth experimenting with.\n\n## The Story Hits Harder With Age\nLavos. The Ocean Palace. Magus's true motivations. Scenes that gutted you at age 10 gut you differently at 30 — because now you understand what they're actually about.\n\n## Verdict\nA flawless JRPG that invented mechanics still being copied. 10/10. Play it.", author = "RetroReviewer", category = "REVIEW", likeCount = 72, viewCount = 610, readingTimeMinutes = 5, date = "Jun 5, 2026")
)

val consoleFilterChips = listOf("🕹️ ALL", "SNES", "NES", "PS1", "PS2", "N64", "SEGA", "GBA", "ARCADE", "PC", "GB")

val friendsArePlayingData = listOf(
    Triple("PixelDave", null as String?, "Super Mario World"),
    Triple("RetroQueenX", null as String?, "Final Fantasy VII"),
    Triple("SynthRider88", null as String?, "Sonic 2"),
    Triple("NintendoNerd", null as String?, "Zelda: LttP"),
    Triple("PolyPioneer", null as String?, "Crash Bandicoot"),
    Triple("BitCrusher", null as String?, "DK Country"),
    Triple("VaultHunter64", null as String?, "GoldenEye 007")
)

fun monthName(month: Int): String = when (month) {
    1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
    5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
    9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
    else -> "Jul"
}

// ─── Shimmer / Loading / Divider Utilities ───────────────────────────────────

@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = -600f, targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerOffset"
    )
    return Brush.linearGradient(
        colors = listOf(ComicGlassBg, ComicGlassBg, ComicGlassBg),
        start = Offset(offset, 0f),
        end = Offset(offset + 500f, 0f)
    )
}

@Composable
fun ShimmerCard(modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(12.dp)).background(shimmerBrush()))
}

@Composable
fun TapeStripDivider() {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        repeat(24) { i ->
            Box(
                modifier = Modifier
                    .weight(1f).height(13.dp)
                    .background(if (i % 2 == 0) CGreen.copy(0.20f) else Color.Transparent)
            )
        }
    }
}

@Composable
fun SectionSlideIn(content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(380)) + slideInVertically(animationSpec = tween(380)) { it / 4 }
    ) { content() }
}

// ─────────────────────────────────────────────────────────────────────────────

fun gameEmoji(name: String): String {
    val n = name.lowercase()
    return when {
        n.contains("mario") -> "🍄"
        n.contains("zelda") -> "🗡️"
        n.contains("sonic") -> "💨"
        n.contains("pokemon") || n.contains("pokémon") -> "⚡"
        n.contains("pac-man") || n.contains("pacman") -> "👾"
        n.contains("tetris") -> "🧩"
        n.contains("street fighter") -> "👊"
        n.contains("mortal kombat") -> "💀"
        n.contains("castlevania") -> "🧛"
        n.contains("mega man") || n.contains("megaman") -> "🤖"
        n.contains("metroid") -> "🚀"
        n.contains("donkey kong") -> "🦍"
        n.contains("final fantasy") -> "⚔️"
        n.contains("sega") -> "🎮"
        n.contains("retro") -> "🕹️"
        n.contains("pixel") -> "🖼️"
        n.contains("speedrun") -> "⚡"
        n.contains("horror") -> "👻"
        else -> "🎮"
    }
}

// ─── Live Data Fetchers ───────────────────────────────────────────────────────

suspend fun fetchGamingHistoryFromWikipedia(month: Int, day: Int): List<GamingHistoryEvent> =
    withContext(Dispatchers.IO) {
        try {
            val mm = month.toString().padStart(2, '0')
            val dd = day.toString().padStart(2, '0')
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://en.wikipedia.org/api/rest_v1/feed/onthisday/events/$mm/$dd")
                .addHeader("User-Agent", "RetroHubApp/1.0")
                .build()
            val body = client.newCall(request).execute().body?.string() ?: return@withContext emptyList()
            val events = org.json.JSONObject(body).optJSONArray("events") ?: return@withContext emptyList()
            val keywords = setOf("nintendo", "sega", "sony playstation", "atari", "game boy",
                "video game", "arcade", "famicom", "mario", "zelda", "sonic", "pokemon",
                "capcom", "konami", "square", "namco", "mega drive", "playstation",
                "n64", "super nintendo", "tetris", "pac-man", "donkey kong", "street fighter",
                "mortal kombat", "final fantasy", "xbox", "game cube", "gameboy")
            val result = mutableListOf<GamingHistoryEvent>()
            for (i in 0 until events.length()) {
                if (result.size >= 6) break
                val ev = events.getJSONObject(i)
                val text = ev.optString("text", "")
                val year = ev.optInt("year", 0)
                if (year < 1970 || year > 2020) continue
                val lower = text.lowercase()
                if (keywords.none { lower.contains(it) }) continue
                val console = when {
                    lower.contains("famicom") || lower.contains("nes ") -> "NES"
                    lower.contains("super nintendo") || lower.contains("snes") -> "SNES"
                    lower.contains("nintendo 64") || lower.contains("n64") -> "N64"
                    lower.contains("game boy") || lower.contains("gameboy") -> "GB"
                    lower.contains("game cube") || lower.contains("gamecube") -> "GCN"
                    lower.contains("playstation 2") -> "PS2"
                    lower.contains("playstation") -> "PS1"
                    lower.contains("mega drive") || lower.contains("genesis") || lower.contains("sega") -> "SEGA"
                    lower.contains("atari") -> "ATARI"
                    lower.contains("arcade") -> "ARCADE"
                    else -> "GAME"
                }
                val title = text.split(".").firstOrNull()?.trim()?.let {
                    if (it.length > 52) it.take(49) + "…" else it
                } ?: "On This Day: $year"
                result.add(GamingHistoryEvent(year, month, day, title, text.take(160), console, gameEmoji(lower)))
            }
            result
        } catch (e: Exception) { emptyList() }
    }

suspend fun fetchGamingQuotesFromFirestore(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
    try {
        val db = FirebaseFirestore.getInstance()
        val snapshot = db.collection("gaming_quotes").orderBy("order").limit(20).get().await()
        if (snapshot.isEmpty) {
            // Auto-seed from hardcoded list on first run
            try {
                val batch = db.batch()
                discoverGamingQuotes.forEachIndexed { i, (quote, source) ->
                    batch.set(db.collection("gaming_quotes").document("q${i.toString().padStart(2, '0')}"),
                        mapOf("quote" to quote, "source" to source, "order" to i))
                }
                batch.commit().await()
            } catch (e: Exception) { /* silent — console can seed manually */ }
            discoverGamingQuotes
        } else {
            snapshot.documents.mapNotNull { doc ->
                val q = doc.getString("quote") ?: return@mapNotNull null
                val s = doc.getString("source") ?: return@mapNotNull null
                q to s
            }
        }
    } catch (e: Exception) { discoverGamingQuotes }
}

suspend fun computeTrendingFromPosts(): Pair<List<TrendingTag>, List<TrendingTag>> = withContext(Dispatchers.IO) {
    try {
        val db = FirebaseFirestore.getInstance()
        val posts = db.collection("posts").orderBy("timestamp", Query.Direction.DESCENDING).limit(300).get().await()
        val tagCounts = mutableMapOf<String, Int>()
        val hashtagRegex = Regex("#\\w+")
        posts.documents.forEach { doc ->
            val content = doc.getString("content") ?: doc.getString("body") ?: ""
            hashtagRegex.findAll(content).forEach { m -> tagCounts[m.value] = (tagCounts[m.value] ?: 0) + 1 }
            val gameName = doc.getString("gameName") ?: ""
            if (gameName.isNotBlank()) {
                val tag = "#${gameName.replace(" ", "")}"
                tagCounts[tag] = (tagCounts[tag] ?: 0) + 1
            }
        }
        if (tagCounts.isEmpty()) return@withContext Pair(hotTrendingTags, risingTrendingTags)
        val sorted = tagCounts.entries.sortedByDescending { it.value }
        val hot = sorted.take(6).mapIndexed { i, (tag, count) ->
            val growth = when (i) { 0 -> "↑ #1 this week"; 1 -> "↑ Surging"; 2 -> "↑ Trending"; else -> "↑ Popular" }
            TrendingTag(tag, "Trending in RetroHub", growth, count, true)
        }
        val rising = sorted.drop(6).take(6).mapIndexed { i, (tag, count) ->
            TrendingTag(tag, "Rising in the community", "↑ Growing fast", count, false)
        }
        Pair(hot.ifEmpty { hotTrendingTags }, rising.ifEmpty { risingTrendingTags })
    } catch (e: Exception) { Pair(hotTrendingTags, risingTrendingTags) }
}

// Derive community pulse from Twitch streams (pure function, call inside remember)
fun communityPulseFromTwitch(streams: List<TwitchStream>): List<CommunityPulseItem> {
    val colors = listOf(CAcPurple, CAcYellow, CAcRed, CAcBlue)
    return streams.groupBy { it.gameName }
        .entries.sortedByDescending { e -> e.value.sumOf { it.viewerCount } }
        .take(4).mapIndexed { idx, (gameName, streamList) ->
            CommunityPulseItem(gameName.take(22), gameEmoji(gameName), streamList.sumOf { it.viewerCount }, (idx + 1) * 2, colors[idx % colors.size])
        }
}

// ─── News Section ─────────────────────────────────────────────────────────────

@Composable
fun NewsSection(
    allNews: List<NewsItem>,
    isLoadingNews: Boolean,
    onNewsClick: (NewsItem) -> Unit
) {
    var selectedTab by remember { mutableStateOf("LATEST") }
    var visibleCount by remember { mutableStateOf(newsPageSize) }

    val filteredNews = remember(allNews, selectedTab) {
        when (selectedTab) {
            "IGN" -> allNews.filter { it.source == "IGN" }
            "KOTAKU" -> allNews.filter { it.source == "Kotaku" }
            "EUROGAMER" -> allNews.filter { it.source == "Eurogamer" }
            "TRENDING" -> allNews.sortedByDescending { it.title.length }
            else -> allNews
        }
    }

    val heroNews = filteredNews.firstOrNull()
    val listNews = filteredNews.drop(1).take(visibleCount)
    val hasMore = filteredNews.drop(1).size > visibleCount

    // ✅ Neon pulse for section
    val neonAlpha by rememberGlowRange(0.4f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DiscoverSectionRow(title = "BREAKING NEWS", emoji = "📡", onSeeAll = null, neonAlpha = neonAlpha)

        // ✅ Tab filter chips with neon border on selected
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
            itemsIndexed(listOf("LATEST", "TRENDING", "IGN", "KOTAKU", "EUROGAMER")) { jumpIndex, tab ->
                Box(modifier = Modifier.jumpIn(jumpIndex)) {
                val isSelected = selectedTab == tab
                val tabColor = when (tab) { "IGN" -> CAcRed; "KOTAKU" -> CGreen; "EUROGAMER" -> CAcBlue; else -> CGreen }
                var pressed by remember { mutableStateOf(false) }
                val tabScale by animateFloatAsState(targetValue = if (pressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "tabChipScale")
                Box(
                    modifier = Modifier
                        .scale(tabScale)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) CGreen else ScrapbookDark.copy(alpha = 0.06f))
                        .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(20.dp))
                        .clickable { pressed = true; selectedTab = tab; visibleCount = newsPageSize }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(tab, fontFamily = BangersFontFamily, color = if (isSelected) ScrapbookDark else when (tab) { "IGN" -> CAcRed; "KOTAKU" -> CGreenDeep; "EUROGAMER" -> CAcBlue; else -> ScrapbookTextMuted }, fontSize = 12.sp)
                }
                LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
                            }
            }
        }

        when {
            isLoadingNews -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(3) { ShimmerCard(modifier = Modifier.fillMaxWidth().height(88.dp)) }
                }
            }
            filteredNews.isEmpty() -> {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookBorder, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📡", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("No news from $selectedTab right now", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
                    }
                }
            }
            else -> {
                heroNews?.let { hero ->
                    // EXTRA! EXTRA! — the top story spins in like a thrown newspaper
                    ExtraExtraBanner()
                    Box(modifier = Modifier.newspaperSpin(hero.title)) {
                        NewsHeroCard(news = hero, isBookmarked = false, onClick = { onNewsClick(hero) }, neonAlpha = neonAlpha)
                    }
                }
                listNews.forEach { news -> NewsListCard(news = news, onClick = { onNewsClick(news) }) }
                if (hasMore) {
                    var viewMorePressed by remember { mutableStateOf(false) }
                    val viewMoreScale by animateFloatAsState(targetValue = if (viewMorePressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "vmScale")
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).scale(viewMoreScale)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CGreen)
                            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(12.dp))
                            .clickable { viewMorePressed = true; visibleCount += newsPageSize }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("VIEW MORE STORIES →", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, letterSpacing = 1.sp)
                    }
                    LaunchedEffect(viewMorePressed) { if (viewMorePressed) { delay(150); viewMorePressed = false } }
                } else if (filteredNews.size > 1) {
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.92f)).border(1.dp, ScrapbookBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(10.dp), contentAlignment = Alignment.Center) {
                        Text("📡 You're all caught up!", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ─── News List Card ───────────────────────────────────────────────────────────

@Composable
fun NewsListCard(news: NewsItem, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "newsListScale")
    Box(modifier = Modifier.scale(cardScale)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth().clickable { pressed = true; onClick() }, backgroundColor = ComicGlassBg, cornerRadius = 12.dp, shadowOffset = 3.dp) {
            Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
                    if (!news.imageUrl.isNullOrBlank()) {
                        AsyncImage(model = news.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(news.imageUrl).fillMaxSize())
                    } else { Text("📰", fontSize = 28.sp) }
                    Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp).background(news.sourceColor))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(news.sourceColor).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(news.source, fontFamily = BangersFontFamily, color = Color.White, fontSize = 9.sp)
                        }
                        if (news.publishedAt.isNotBlank()) Text(news.publishedAt, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(news.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp, letterSpacing = 0.2.sp)
                    if (news.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(news.description, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ScrapbookDark.copy(alpha = 0.25f), modifier = Modifier.size(20.dp))
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── News Hero Card ───────────────────────────────────────────────────────────

@Composable
fun NewsHeroCard(news: NewsItem, isBookmarked: Boolean = false, onClick: () -> Unit, neonAlpha: Float = 0.6f) {
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "heroScale")
    Box(modifier = Modifier.scale(cardScale)) {
        // ✅ Neon glow behind hero card
        Box(modifier = Modifier.matchParentSize().padding(4.dp).blur(16.dp).background(news.sourceColor.copy(alpha = neonAlpha * 0.3f), RoundedCornerShape(16.dp)))
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth()
                .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(16.dp))
                .clickable { pressed = true; onClick() },
            backgroundColor = ScrapbookDark, cornerRadius = 16.dp, shadowOffset = 5.dp
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                if (!news.imageUrl.isNullOrBlank()) {
                    AsyncImage(model = news.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(news.imageUrl).fillMaxSize(), alpha = 0.5f)
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(CGreen, CGreenDeep))))
                }
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.85f)))))
                Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(CAcRed).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("🔴 BREAKING", fontFamily = BangersFontFamily, color = Color.White, fontSize = 12.sp, letterSpacing = 1.sp)
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(news.sourceColor).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text(news.source.uppercase(), fontFamily = BangersFontFamily, color = Color.White, fontSize = 12.sp)
                        }
                    }
                    Column {
                        Text(news.title, fontFamily = BangersFontFamily, color = Color.White, fontSize = 22.sp, lineHeight = 26.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, letterSpacing = 0.3.sp)
                        if (news.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(news.description, fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            if (news.publishedAt.isNotBlank()) Text("🕐 ${news.publishedAt}", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(CGreen)
                                    .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("READ INSIDE APP →", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── News Compact Card ────────────────────────────────────────────────────────

@Composable
fun NewsCompactCard(news: NewsItem, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.95f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "compactScale")
    Box(modifier = Modifier.width(200.dp).scale(cardScale)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth().clickable { pressed = true; onClick() }, backgroundColor = ComicGlassBg, cornerRadius = 12.dp, shadowOffset = 3.dp) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(Color.White.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
                    if (!news.imageUrl.isNullOrBlank()) { AsyncImage(model = news.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(news.imageUrl).fillMaxSize()) } else { Text("📰", fontSize = 32.sp) }
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(4.dp)).background(news.sourceColor).padding(horizontal = 6.dp, vertical = 2.dp)) { Text(news.source, fontFamily = BangersFontFamily, color = Color.White, fontSize = 9.sp) }
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)))))
                }
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(news.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                    if (news.publishedAt.isNotBlank()) { Spacer(modifier = Modifier.height(4.dp)); Text(news.publishedAt, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp) }
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── News Reader Screen ───────────────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NewsReaderScreen(news: NewsItem, currentUserId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var isBookmarked by remember { mutableStateOf(false) }
    var articleContent by remember { mutableStateOf("") }
    var isLoadingContent by remember { mutableStateOf(true) }
    var useWebView by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isWebViewLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentUserId, news.url) {
        if (currentUserId.isNotBlank()) {
            try {
                val docId = news.url.hashCode().toString()
                val doc = FirebaseFirestore.getInstance().collection("users").document(currentUserId).collection("saved_news").document(docId).get().await()
                isBookmarked = doc.exists()
            } catch (e: Exception) { }
        }
    }

    LaunchedEffect(news.url) {
        isLoadingContent = true
        val content = fetchArticleContent(news.url)
        if (content.length > 200) { articleContent = content; useWebView = false } else { useWebView = true }
        isLoadingContent = false
    }

    fun toggleBookmark() {
        if (currentUserId.isBlank()) return
        val docId = news.url.hashCode().toString()
        val ref = FirebaseFirestore.getInstance().collection("users").document(currentUserId).collection("saved_news").document(docId)
        if (isBookmarked) { ref.delete(); isBookmarked = false }
        else { ref.set(mapOf("title" to news.title, "url" to news.url, "imageUrl" to (news.imageUrl ?: ""), "source" to news.source, "publishedAt" to news.publishedAt, "savedAt" to System.currentTimeMillis())); isBookmarked = true }
    }

    fun shareArticle() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, news.title); putExtra(Intent.EXTRA_TEXT, "${news.title}\n\n${news.url}") }
        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    Box(modifier = Modifier.fillMaxSize().background(ComicGlassBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ✅ Dark top bar with neon accent
            Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(CGreen, CGreenMint, CGreen))).padding(top = 40.dp, bottom = 12.dp, start = 4.dp, end = 8.dp)) {
                // Yellow accent line at top
                Box(modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter).background(Brush.horizontalGradient(colors = listOf(Color.Transparent, ScrapbookDark.copy(alpha = 0.6f), ScrapbookDark, ScrapbookDark.copy(alpha = 0.6f), Color.Transparent))))
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ScrapbookDark) }
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(news.sourceColor).border(1.5.dp, ScrapbookDark, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text(news.source, fontFamily = BangersFontFamily, color = Color.White, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { toggleBookmark() }) {
                        Icon(imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = "Bookmark", tint = ScrapbookDark)
                    }
                    IconButton(onClick = { shareArticle() }) { Icon(Icons.Filled.Share, contentDescription = "Share", tint = ScrapbookDark) }
                    IconButton(onClick = { val intent = Intent(Intent.ACTION_VIEW, Uri.parse(news.url)); try { context.startActivity(intent) } catch (e: Exception) { } }) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = "Open in browser", tint = ScrapbookDark.copy(alpha = 0.7f))
                    }
                }
            }

            when {
                isLoadingContent -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ThreeDotsAnimation()
                            Text("Loading article...", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 14.sp)
                        }
                    }
                }
                useWebView -> {
                    if (isWebViewLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = CGreenDeep, trackColor = Color.White.copy(alpha = 0.46f))
                    AndroidView(factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            settings.apply { javaScriptEnabled = true; domStorageEnabled = true; loadWithOverviewMode = true; useWideViewPort = true }
                            webViewClient = object : WebViewClient() { override fun onPageFinished(view: WebView?, url: String?) { isWebViewLoading = false } }
                            webChromeClient = WebChromeClient(); loadUrl(news.url); webViewRef = this
                        }
                    }, modifier = Modifier.fillMaxSize())
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
                        item {
                            if (!news.imageUrl.isNullOrBlank()) {
                                Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                                    AsyncImage(model = news.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(news.imageUrl).fillMaxSize())
                                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, ComicGlassBg.copy(alpha = 0.9f)))))
                                }
                            }
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(news.sourceColor).padding(horizontal = 8.dp, vertical = 3.dp)) { Text(news.source, fontFamily = BangersFontFamily, color = Color.White, fontSize = 11.sp) }
                                    if (news.publishedAt.isNotBlank()) Text("🕐 ${news.publishedAt}", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(news.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = 0.3.sp)
                                if (news.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(CGreen.copy(alpha = 0.15f)).border(2.dp, CGreenDeep, RoundedCornerShape(8.dp)).padding(12.dp)) {
                                        Text(news.description, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark.copy(alpha = 0.75f), fontSize = 14.sp, lineHeight = 20.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(articleContent, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.80f), fontSize = 16.sp, lineHeight = 26.sp)
                                Spacer(modifier = Modifier.height(24.dp))
                                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CGreen).border(2.5.dp, ScrapbookDark, RoundedCornerShape(12.dp)).clickable { val intent = Intent(Intent.ACTION_VIEW, Uri.parse(news.url)); try { context.startActivity(intent) } catch (e: Exception) { } }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Filled.OpenInBrowser, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(18.dp))
                                        Text("OPEN FULL ARTICLE →", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
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

// ─── Game Deal Card ───────────────────────────────────────────────────────────

@Composable
fun GameDealCard(deal: GameDeal) {
    val savingsColor = when { deal.savings >= 70 -> CGreenDeep; deal.savings >= 40 -> CAcYellow; else -> ScrapbookTextMuted }
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "dealScale")

    // ✅ Neon glow on high-savings deals
    val dealNeon by rememberGlowRange(0.3f, 0.9f)

    Box(modifier = Modifier.width(160.dp).scale(cardScale).clickable { pressed = true }) {
        if (deal.savings >= 50) {
            Box(modifier = Modifier.matchParentSize().padding(3.dp).blur(10.dp).background(savingsColor.copy(alpha = dealNeon * 0.4f), RoundedCornerShape(12.dp)))
        }
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth()
                .then(if (deal.savings >= 50) Modifier.border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(12.dp)) else Modifier),
            backgroundColor = ComicGlassBg, cornerRadius = 12.dp, shadowOffset = 3.dp
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(Color.White.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
                    if (deal.thumb.isNotBlank()) { AsyncImage(model = deal.thumb, contentDescription = deal.title, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(deal.thumb).fillMaxSize()) } else { Text("🎮", fontSize = 32.sp) }
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(6.dp)).background(savingsColor).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("-${deal.savings}%", fontFamily = BangersFontFamily, color = Color.White, fontSize = 12.sp) }
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(4.dp)).background(ScrapbookDark.copy(alpha = 0.8f)).padding(horizontal = 5.dp, vertical = 2.dp)) { Text(storeName(deal.storeId), fontFamily = BangersFontFamily, color = Color.White, fontSize = 8.sp) }
                }
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(deal.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("$${deal.salePrice}", fontFamily = BangersFontFamily, color = savingsColor, fontSize = 16.sp)
                        Text("$${deal.normalPrice}", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                    }
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Retro Game of the Day Card ───────────────────────────────────────────────

@Composable
fun RetroGameOfDayCard(game: RetroGameOfDay, onViewInDatabase: () -> Unit) {
    val neonAlpha by rememberGlowRange(0.3f, 0.9f)
    val btnScale by rememberGlowRange(1f, 1.04f)

    Box {
        // ✅ Neon glow behind card
        Box(modifier = Modifier.matchParentSize().padding(4.dp).blur(14.dp).background(CGreen.copy(alpha = neonAlpha * 0.2f), RoundedCornerShape(16.dp)))
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth()
                .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(16.dp)),
            backgroundColor = ComicGlassBg, cornerRadius = 16.dp, shadowOffset = 5.dp
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                if (!game.coverUrl.isNullOrBlank()) { AsyncImage(model = game.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(game.coverUrl).fillMaxSize(), alpha = 0.12f) }
                Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(colors = listOf(ComicGlassBg.copy(alpha = 0.97f), ComicGlassBg.copy(alpha = 0.85f)))))
                Row(modifier = Modifier.fillMaxSize().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(modifier = Modifier.size(110.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        if (!game.coverUrl.isNullOrBlank()) { AsyncImage(model = game.coverUrl, contentDescription = game.name, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(game.coverUrl).fillMaxSize()) } else { Text("🎮", fontSize = 40.sp) }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(CGreen).padding(horizontal = 8.dp, vertical = 3.dp)) { Text("🎮 GAME OF THE DAY", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 10.sp) }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(game.name, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, lineHeight = 23.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        game.releaseYear?.let { Text("$it", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp) }
                        game.rating?.let { rating ->
                            Spacer(modifier = Modifier.height(4.dp))
                            val score = (rating / 10).toInt()
                            val ratingColor = when { rating >= 80 -> CGreenDeep; rating >= 60 -> CAcYellow; else -> CAcRed }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                HoloBadge(label = "$score/10", cornerRadius = 5.dp, fontSize = 12.sp)
                                Text(when { rating >= 80 -> "⭐ OUTSTANDING"; rating >= 70 -> "👍 GREAT"; rating >= 60 -> "✅ GOOD"; else -> "😐 MIXED" }, fontFamily = BangersFontFamily, color = ratingColor, fontSize = 12.sp)
                            }
                        }
                        game.summary?.let { Text(it, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 15.sp) }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.scale(btnScale).clip(RoundedCornerShape(8.dp)).background(CGreen).border(2.dp, ScrapbookDark.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).clickable { onViewInDatabase() }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("VIEW IN DATABASE →", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
// ─── Discover Screen ──────────────────────────────────────────────────────────

@Composable
fun DiscoverScreen(
    authViewModel: AuthViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(),
    streamsViewModel: StreamsViewModel = viewModel(),
    onNavigateToAlbums: () -> Unit = {},
    onNavigateToMagazines: () -> Unit = {},
    onNavigateToArticles: () -> Unit = {},
    onNavigateToStreams: () -> Unit = {},
    onNavigateToGameDatabase: () -> Unit = {},
    onNavigateToEvents: () -> Unit = {},
    onNavigateToMarketplace: () -> Unit = {},
    onNavigateToCheckpoints: () -> Unit = {},
    onNavigateToRetroBytes: () -> Unit = {},
    achievementsViewModel: AchievementsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val discoverContext = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val followingUids by authViewModel.followingUids.collectAsState()
    val twitchState by streamsViewModel.twitchStreams.collectAsState()
    val communityStreamers by streamsViewModel.communityStreamers.collectAsState()
    val allUsers by authViewModel.allUsers.collectAsState()

    var discoverFilter by remember { mutableStateOf("ALL") }
    var selectedConsole by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var hasSearched by remember { mutableStateOf(false) }
    var isSearchingUsers by remember { mutableStateOf(false) }
    var realUsers by remember { mutableStateOf<List<UserProfileData>>(emptyList()) }
    var selectedUser by remember { mutableStateOf<UserProfileData?>(null) }
    var selectedNews by remember { mutableStateOf<NewsItem?>(null) }
    var recentSearches by remember { mutableStateOf<List<String>>(emptyList()) }

    var trendingUsers by remember { mutableStateOf<List<UserProfileData>>(emptyList()) }
    var recentArticles by remember { mutableStateOf<List<ArticleItem>>(emptyList()) }
    var featuredArticle by remember { mutableStateOf<ArticleItem?>(null) }
    var topPlayers by remember { mutableStateOf<List<UserProfileData>>(emptyList()) }
    var isLoadingTrending by remember { mutableStateOf(true) }

    var newsList by remember { mutableStateOf<List<NewsItem>>(emptyList()) }
    var isLoadingNews by remember { mutableStateOf(true) }
    var gameDeals by remember { mutableStateOf<List<GameDeal>>(emptyList()) }
    var isLoadingDeals by remember { mutableStateOf(true) }
    var gameOfDay by remember { mutableStateOf<RetroGameOfDay?>(null) }
    var isLoadingGameOfDay by remember { mutableStateOf(true) }

    val liveStreamers = remember(twitchState, communityStreamers) {
        if (twitchState is StreamsState.Success<*>) {
            @Suppress("UNCHECKED_CAST")
            val streams = (twitchState as StreamsState.Success<TwitchStream>).data
            communityStreamers.filter { streamer -> streams.any { it.userName.lowercase() == streamer.twitchUsername.lowercase() } }.take(3)
        } else emptyList()
    }

    // ─── Live Data State ──────────────────────────────────────────────────────
    var liveGamingHistory by remember { mutableStateOf<List<GamingHistoryEvent>>(emptyList()) }
    var liveTrendingHot   by remember { mutableStateOf<List<TrendingTag>>(emptyList()) }
    var liveTrendingRising by remember { mutableStateOf<List<TrendingTag>>(emptyList()) }
    var liveFirestorePulse by remember { mutableStateOf<List<CommunityPulseItem>>(emptyList()) }

    // Community pulse: Twitch streams → derive live; fall back to Firestore-computed
    val liveCommunityPulse = remember(twitchState, liveFirestorePulse) {
        val streams = (twitchState as? StreamsState.Success<*>)?.let {
            @Suppress("UNCHECKED_CAST") (it as StreamsState.Success<TwitchStream>).data
        } ?: emptyList()
        if (streams.isNotEmpty()) communityPulseFromTwitch(streams)
        else liveFirestorePulse
    }

    // Friends Are Playing: use followed users' topGames field when available
    val friendsPlayingList = remember(trendingUsers) {
        val fromProfiles = trendingUsers.take(7).mapNotNull { user ->
            val gameName = (user.topGames.firstOrNull() as? Map<*, *>)?.get("name") as? String
                ?: return@mapNotNull null
            Triple(user.username, user.profilePictureUrl ?: "", gameName)
        }
        fromProfiles.ifEmpty { friendsArePlayingData.map { Triple(it.first, it.second ?: "", it.third) } }
    }

    // ✅ Global neon pulse
    val neonAlpha by rememberGlowRange(0.4f, 1f)

    // Follow/unfollow + Daily Quest tracking
    val toggleFollow: (UserProfileData) -> Unit = { user ->
        if (followingUids.contains(user.uid)) authViewModel.unfollowUser(user.uid)
        else {
            authViewModel.followUser(user.uid)
            DailyQuests.report(discoverContext, DailyQuests.FOLLOW, achievementsViewModel)
        }
    }

    // Who-to-follow deck: snapshot candidates once so following someone doesn't reshuffle the deck
    var deckUsers by remember { mutableStateOf<List<UserProfileData>>(emptyList()) }
    var deckFromAllUsers by remember { mutableStateOf(false) }
    LaunchedEffect(trendingUsers, allUsers) {
        fun candidates(src: List<UserProfileData>) = src.distinctBy { it.uid }
            .filter { it.uid != currentUser?.uid && it.username.isNotBlank() && !followingUids.contains(it.uid) }
            .take(15)
        if (!deckFromAllUsers && allUsers.isNotEmpty()) {
            val c = candidates(trendingUsers + allUsers)
            if (c.isNotEmpty()) { deckUsers = c; deckFromAllUsers = true }
        } else if (deckUsers.isEmpty() && trendingUsers.isNotEmpty()) {
            deckUsers = candidates(trendingUsers)
        }
    }

    // Warp Zone: portals to every other page
    val warpDestinations = listOf(
        WarpDestination("GAMES", "👾", CGreenDeep) { onNavigateToGameDatabase() },
        WarpDestination("ALBUMS", "🎵", CGreen) { onNavigateToAlbums() },
        WarpDestination("MAGAZINES", "📰", CGreenMint) { onNavigateToMagazines() },
        WarpDestination("ARTICLES", "✍️", CGreenDeep) { onNavigateToArticles() },
        WarpDestination("STREAMS", "📺", CGreen, badge = if (liveStreamers.isNotEmpty()) "LIVE" else null) { onNavigateToStreams() },
        WarpDestination("EVENTS", "📅", CGreenMint) { onNavigateToEvents() },
        WarpDestination("MARKET", "🛒", CGreenDeep) { onNavigateToMarketplace() },
        WarpDestination("SHORTS", "📱", CGreen) { onNavigateToRetroBytes() }
    ).map { d -> d.copy(onClick = {
        DailyQuests.report(discoverContext, DailyQuests.WARP, achievementsViewModel, uniqueKey = d.label)
        d.onClick()
    }) }

    LaunchedEffect(Unit) {
        isLoadingNews = true
        try { val allNews = rssSources.flatMap { (name, color, url) -> fetchNewsFromRss(name, color, url, limit = 8) }.shuffled(); newsList = allNews } catch (e: Exception) { } finally { isLoadingNews = false }
    }
    LaunchedEffect(Unit) {
        isLoadingDeals = true
        try { gameDeals = fetchGameDeals() } catch (e: Exception) { } finally { isLoadingDeals = false }
    }
    LaunchedEffect(Unit) {
        isLoadingGameOfDay = true
        try { gameOfDay = fetchRetroGameOfDay() } catch (e: Exception) { } finally { isLoadingGameOfDay = false }
    }
    LaunchedEffect(Unit) {
        isLoadingTrending = true
        try {
            val firestore = FirebaseFirestore.getInstance()
            val usersDoc = firestore.collection("users").orderBy("followersCount", Query.Direction.DESCENDING).limit(8).get().await()
            val allFetched = usersDoc.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                if (doc.id == currentUser?.uid) return@mapNotNull null
                UserProfileData(uid = doc.id, username = data["username"] as? String ?: "", userHandle = data["userHandle"] as? String ?: "", bio = data["bio"] as? String ?: "", email = data["email"] as? String ?: "", profilePictureUrl = data["profilePictureUrl"] as? String ?: "", bannerUrl = data["bannerUrl"] as? String ?: "", followersCount = (data["followersCount"] as? Long)?.toInt() ?: 0, followingCount = (data["followingCount"] as? Long)?.toInt() ?: 0, setupComplete = data["setupComplete"] as? Boolean ?: false, topGames = (data["topGames"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList(), topSoundtracks = (data["topSoundtracks"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList(), twitchUsername = data["twitchUsername"] as? String ?: "", youtubeUsername = data["youtubeUsername"] as? String ?: "")
            }
            trendingUsers = allFetched.take(5); topPlayers = allFetched.take(3)
            try {
                val featuredDoc = firestore.collection("articles").orderBy("viewCount", Query.Direction.DESCENDING).limit(1).get().await()
                featuredArticle = featuredDoc.documents.firstOrNull()?.let { doc -> val data = doc.data ?: return@let null; ArticleItem(id = doc.id, title = data["title"] as? String ?: "", snippet = data["snippet"] as? String ?: "", fullContent = data["fullContent"] as? String ?: "", author = data["authorUsername"] as? String ?: "", imageUrl = data["headerImageUrl"] as? String) }?.takeIf { it.title.isNotBlank() }
            } catch (e: Exception) {
                val fallback = firestore.collection("articles").orderBy("timestamp", Query.Direction.DESCENDING).limit(1).get().await()
                featuredArticle = fallback.documents.firstOrNull()?.let { doc -> val data = doc.data ?: return@let null; ArticleItem(id = doc.id, title = data["title"] as? String ?: "", snippet = data["snippet"] as? String ?: "", fullContent = data["fullContent"] as? String ?: "", author = data["authorUsername"] as? String ?: "", imageUrl = data["headerImageUrl"] as? String) }?.takeIf { it.title.isNotBlank() }
            }
            val articlesDoc = firestore.collection("articles").orderBy("timestamp", Query.Direction.DESCENDING).limit(6).get().await()
            recentArticles = articlesDoc.documents.mapNotNull { doc -> val data = doc.data ?: return@mapNotNull null; ArticleItem(id = doc.id, title = data["title"] as? String ?: "", snippet = data["snippet"] as? String ?: "", fullContent = data["fullContent"] as? String ?: "", author = data["authorUsername"] as? String ?: "", imageUrl = data["headerImageUrl"] as? String) }.filter { it.title.isNotBlank() }
        } catch (e: Exception) { } finally { isLoadingTrending = false }
        authViewModel.fetchAllUsers()
    }

    LaunchedEffect(allUsers) { streamsViewModel.loadCommunityStreamers(allUsers) }

    // ─── Live content fetching ────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        // 1. Wikipedia "On This Day" → gaming history events
        val cal = java.util.Calendar.getInstance()
        val wikiHistory = fetchGamingHistoryFromWikipedia(
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        )
        if (wikiHistory.isNotEmpty()) liveGamingHistory = wikiHistory

        // 2. Firestore posts → trending hashtags (computed, not hardcoded)
        val (hot, rising) = computeTrendingFromPosts()
        if (hot.isNotEmpty()) liveTrendingHot = hot
        if (rising.isNotEmpty()) liveTrendingRising = rising

        // 3. Firestore posts → community pulse fallback (Twitch is primary)
        try {
            val db = FirebaseFirestore.getInstance()
            val oneDayAgo = System.currentTimeMillis() - 24 * 3600 * 1000L
            val posts = db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100).get().await()
            val topicCounts = mutableMapOf<String, Pair<Int, Long>>()
            posts.documents.forEach { doc ->
                val gn = doc.getString("gameName")?.takeIf { it.isNotBlank() } ?: return@forEach
                val ts = doc.getLong("timestamp") ?: 0L
                val cur = topicCounts[gn] ?: Pair(0, 0L)
                topicCounts[gn] = Pair(cur.first + 1, maxOf(cur.second, ts))
            }
            val colors = listOf(CAcPurple, CAcYellow, CAcRed, CAcBlue)
            val pulse = topicCounts.entries.sortedByDescending { it.value.first }.take(4)
                .mapIndexed { i, (topic, pair) ->
                    val minsAgo = ((System.currentTimeMillis() - pair.second) / 60_000L).toInt().coerceAtLeast(1)
                    CommunityPulseItem(topic.take(22), gameEmoji(topic), pair.first, minsAgo, colors[i % colors.size])
                }
            if (pulse.isNotEmpty()) liveFirestorePulse = pulse
        } catch (e: Exception) { /* keep default */ }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            delay(600); isSearchingUsers = true
            if (searchQuery.length >= 3) DailyQuests.report(discoverContext, DailyQuests.SEARCH, achievementsViewModel)
            if (searchQuery.length >= 3 && !recentSearches.contains(searchQuery)) {
                recentSearches = (listOf(searchQuery) + recentSearches).take(5)
            }
            try {
                val firestore = FirebaseFirestore.getInstance()
                val byUsername = firestore.collection("users").whereGreaterThanOrEqualTo("username", searchQuery.lowercase()).whereLessThanOrEqualTo("username", searchQuery.lowercase() + "\uf8ff").limit(10).get().await()
                val byHandle = firestore.collection("users").whereGreaterThanOrEqualTo("userHandle", "@${searchQuery.lowercase()}").whereLessThanOrEqualTo("userHandle", "@${searchQuery.lowercase()}\uf8ff").limit(10).get().await()
                realUsers = (byUsername.documents + byHandle.documents).distinctBy { it.id }.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    UserProfileData(uid = doc.id, username = data["username"] as? String ?: "", userHandle = data["userHandle"] as? String ?: "", bio = data["bio"] as? String ?: "", email = data["email"] as? String ?: "", profilePictureUrl = data["profilePictureUrl"] as? String ?: "", bannerUrl = data["bannerUrl"] as? String ?: "", followersCount = (data["followersCount"] as? Long)?.toInt() ?: 0, followingCount = (data["followingCount"] as? Long)?.toInt() ?: 0, setupComplete = data["setupComplete"] as? Boolean ?: false, topGames = (data["topGames"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList(), topSoundtracks = (data["topSoundtracks"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList())
                }.filter { it.uid != currentUser?.uid }
            } catch (e: Exception) { realUsers = emptyList() } finally { isSearchingUsers = false }
        } else { realUsers = emptyList() }
    }

    val localResults: List<DiscoverResult> = remember {
        sampleMagazineCovers.map { DiscoverResult(id = "mag_${it.id}", title = it.title, subtitle = "Virtual Magazine", category = DiscoverCategory.MAGAZINE) } +
                sampleAlbums.map { DiscoverResult(id = "alb_${it.id}", title = it.title, subtitle = it.artist, category = DiscoverCategory.ALBUM) }
    }

    val filteredLocal = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else localResults.filter { it.title.contains(searchQuery, ignoreCase = true) || it.subtitle.contains(searchQuery, ignoreCase = true) }
    }

    val groupedLocal = filteredLocal.groupBy { it.category }

    if (selectedNews != null) {
        NewsReaderScreen(news = selectedNews!!, currentUserId = currentUser?.uid ?: "", onBack = { selectedNews = null }); return
    }
    if (selectedUser != null) {
        UserProfileViewScreen(user = selectedUser!!, authViewModel = authViewModel, onBack = { selectedUser = null }); return
    }

    Box(modifier = modifier.fillMaxSize().background(ComicGlassBg)) {
        HalftoneBackground(modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────────────
            ComicPageHeader(title = "DISCOVER", subtitle = "News, deals, people & more", marquee = pageMarqueeFor("DISCOVER")) {
                if (!isLoadingNews && newsList.isNotEmpty()) {
                    val liveScale by rememberGlowRange(1f, 1.06f)
                    Box(modifier = Modifier.scale(liveScale).clip(RoundedCornerShape(8.dp)).background(ScrapbookDark).border(1.dp, CGreen.copy(alpha = neonAlpha * 0.6f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(CGreenMint))
                            Text("LIVE FEED", fontFamily = BangersFontFamily, color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }

            // ✅ Search bar
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp)
                    .background(ScrapbookDark.copy(alpha = 0.12f)))
            Box(modifier = Modifier.fillMaxWidth().background(ComicGlassBg).padding(horizontal = 16.dp, vertical = 10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it; hasSearched = it.isNotBlank() },
                    placeholder = {
                        RotatingTypewriterHint(
                            hints = listOf("Search players, games, articles…", "Try: Chrono Trigger", "Try: Sonic OST", "Try: EGM 1995", "Try: a player's @handle"),
                            style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextMuted)
                        )
                    },
                    leadingIcon = {
                        if (isSearchingUsers) CircularProgressIndicator(color = ScrapbookDark, modifier = Modifier.size(20.dp).padding(2.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Filled.Search, contentDescription = null, tint = ScrapbookTextMuted, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = ""; hasSearched = false; realUsers = emptyList(); focusManager.clearFocus() }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear", tint = ScrapbookTextMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookDark),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CGreen, unfocusedBorderColor = ScrapbookBorder.copy(alpha = 0.35f), focusedContainerColor = Color.White.copy(alpha = 0.80f), unfocusedContainerColor = Color.White.copy(alpha = 0.55f), cursorColor = ScrapbookDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                ComicShimmer(Modifier.matchParentSize(), alpha = 0.35f)
            }
            } // end search bar shadow wrapper

            // ✅ Search history chips (shown when search bar empty)
            if (!hasSearched) {
                SearchHistoryChips(recentSearches = recentSearches, onChipClick = { searchQuery = it; hasSearched = true }, onRemove = { recentSearches = recentSearches.filter { s -> s != it } })
            }

            // ✅ Browse by Console + category filter chips
            if (!hasSearched) {
                BrowseByConsolePills(selectedConsole = selectedConsole, onConsoleSelected = { selectedConsole = it })
                LazyRow(modifier = Modifier.fillMaxWidth().background(ComicGlassBg).padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(discoverFilters) { jumpIndex, filter ->
                        Box(modifier = Modifier.jumpIn(jumpIndex)) {
                        val isSelected = discoverFilter == filter
                        var pressed by remember { mutableStateOf(false) }
                        val chipScale by animateFloatAsState(targetValue = if (pressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "chipScale")
                        Box {
                            if (isSelected) {
                                GlowPulse(Modifier.matchParentSize(), glowColor = CGreen, cornerRadius = 20.dp)
                            }
                        Box(
                            modifier = Modifier.scale(chipScale).clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) CGreen else ScrapbookDark.copy(alpha = 0.06f))
                                .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(20.dp))
                                .clickable { pressed = true; discoverFilter = filter }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(filter, fontFamily = BangersFontFamily, color = if (isSelected) ScrapbookDark else ScrapbookTextMuted, fontSize = 13.sp)
                        }
                        } // end chip wrapper
                        LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
                                            }
                    }
                }
            }

            // ✅ Console-filtered data
            val filteredMagazines = remember(selectedConsole) {
                if (selectedConsole == "ALL") sampleMagazineCovers
                else sampleMagazineCovers.filter { it.era.contains(selectedConsole, ignoreCase = true) || it.platform.contains(selectedConsole, ignoreCase = true) }.ifEmpty { sampleMagazineCovers }
            }
            val filteredAlbums = remember(selectedConsole) {
                if (selectedConsole == "ALL") sampleAlbums
                else sampleAlbums.filter { it.era.contains(selectedConsole, ignoreCase = true) }.ifEmpty { sampleAlbums }
            }

            when {
                !hasSearched -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp)) {

                        // 🌟 Featured Story Hero — full-bleed editorial card
                        if (discoverFilter == "ALL") {
                            val heroArticle = featuredArticle
                            if (heroArticle != null) {
                                item {
                                    SectionSlideIn { FeaturedStoryHeroCard(article = heroArticle, neonAlpha = neonAlpha) }
                                }
                                item { TapeStripDivider() }
                            }
                        }

                        // ⚔️ Daily Quests + 🌀 Warp Zone
                        if (discoverFilter == "ALL") {
                            item { SectionSlideIn { DailyQuestsCard() } }
                            item {
                                SectionSlideIn {
                                    Column {
                                        DiscoverSectionRow(title = "WARP ZONE", emoji = "🌀", onSeeAll = null, neonAlpha = neonAlpha)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        WarpZoneGrid(destinations = warpDestinations)
                                    }
                                }
                            }
                            item { TapeStripDivider() }
                        }

                        // ⭐ Picked For You — top of feed (real articles only)
                        if (discoverFilter == "ALL" && recentArticles.isNotEmpty()) {
                            item { SectionSlideIn { PickedForYouSection(currentUserUid = currentUser?.uid, articles = recentArticles, neonAlpha = neonAlpha) } }
                            item { TapeStripDivider() }
                        }

                        // 📡 Breaking News
                        if (discoverFilter == "ALL") {
                            item { SectionSlideIn { NewsSection(allNews = newsList, isLoadingNews = isLoadingNews, onNewsClick = {
                                selectedNews = it
                                DailyQuests.report(discoverContext, DailyQuests.READ_NEWS, achievementsViewModel)
                            }) } }
                            item { TapeStripDivider() }
                        }

                        // 🤝 Who to Follow — swipeable deck
                        if (deckUsers.isNotEmpty() && (discoverFilter == "ALL" || discoverFilter == "PEOPLE")) {
                            item {
                                Column {
                                    DiscoverSectionRow(title = "WHO TO FOLLOW", emoji = "🤝", onSeeAll = null, neonAlpha = neonAlpha)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    SwipeFollowDeck(
                                        users = deckUsers,
                                        onFollow = { user -> if (!followingUids.contains(user.uid)) toggleFollow(user) },
                                        onOpen = { selectedUser = it }
                                    )
                                }
                            }
                            item { TapeStripDivider() }
                        }

                        // 🕹️ Friends Are Playing
                        if (discoverFilter == "ALL" || discoverFilter == "PEOPLE") {
                            item { SectionSlideIn { FriendsArePlayingSection(displayData = friendsPlayingList, neonAlpha = neonAlpha) } }
                        }

                        // 🎮 Retro Game of the Day
                        if (discoverFilter == "ALL") {
                            item {
                                TapeStripDivider()
                                SectionSlideIn {
                                    Column {
                                        DiscoverSectionRow(title = "RETRO GAME OF THE DAY", emoji = "🎮", onSeeAll = { onNavigateToGameDatabase() }, neonAlpha = neonAlpha)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        when {
                                            isLoadingGameOfDay -> ShimmerCard(modifier = Modifier.fillMaxWidth().height(160.dp))
                                            gameOfDay != null -> RetroGameOfDayCard(game = gameOfDay!!, onViewInDatabase = { onNavigateToGameDatabase() })
                                            else -> { }
                                        }
                                    }
                                }
                            }
                        }

                        // 🗓️ This Week in Gaming History
                        if (discoverFilter == "ALL") {
                            item { SectionSlideIn { GamingHistorySection(liveEvents = liveGamingHistory, neonAlpha = neonAlpha) } }
                            item { TapeStripDivider() }
                        }

                        // 🟢 Active Right Now
                        if (discoverFilter == "ALL") {
                            item { SectionSlideIn { ActiveRightNowSection(items = liveCommunityPulse, neonAlpha = neonAlpha) } }
                        }

                        // 🔥 Gaming Deals
                        if ((gameDeals.isNotEmpty() || isLoadingDeals) && discoverFilter == "ALL") {
                            item {
                                TapeStripDivider()
                                DiscoverSectionRow(title = "GAMING DEALS", emoji = "🔥", onSeeAll = null, neonAlpha = neonAlpha)
                                Spacer(modifier = Modifier.height(10.dp))
                                if (isLoadingDeals) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        repeat(3) { ShimmerCard(modifier = Modifier.width(140.dp).height(60.dp)) }
                                    }
                                } else {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 2.dp)) { items(gameDeals, key = { it.title }) { deal -> GameDealCard(deal = deal) } }
                                }
                            }
                        }

                        // 📊 Retro Recap
                        if (discoverFilter == "ALL") {
                            item { TapeStripDivider(); SectionSlideIn { RetroRecapCard(displayName = currentUser?.displayName, neonAlpha = neonAlpha) } }
                        }

                        // 🔥 Trending Hashtags with HOT/RISING
                        if (discoverFilter == "ALL") {
                            item { SectionSlideIn { TrendingHashtagsSection(hotTags = liveTrendingHot, risingTags = liveTrendingRising, neonAlpha = neonAlpha) } }
                        }

                        // 🔴 Live Now
                        if (liveStreamers.isNotEmpty() && (discoverFilter == "ALL" || discoverFilter == "LIVE")) {
                            item {
                                DiscoverSectionRow(title = "LIVE NOW", emoji = "🔴", onSeeAll = { onNavigateToStreams() }, neonAlpha = neonAlpha)
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 2.dp)) { items(liveStreamers, key = { it.uid }) { streamer -> LiveStreamerCard(streamer = streamer) } }
                            }
                        }

                        // 🏆 Top Players
                        if (topPlayers.isNotEmpty() && (discoverFilter == "ALL" || discoverFilter == "PEOPLE")) {
                            item {
                                DiscoverSectionRow(title = "TOP PLAYERS", emoji = "🏆", onSeeAll = null, neonAlpha = neonAlpha)
                                Spacer(modifier = Modifier.height(10.dp))
                                TrendingPlayersCard(users = topPlayers, followingUids = followingUids, currentUid = currentUser?.uid ?: "", onFollowClick = { user -> toggleFollow(user) }, onTap = { selectedUser = it })
                            }
                        }

                        // ⭐ Featured Article
                        val displayFeatured = featuredArticle
                        if (displayFeatured != null && (discoverFilter == "ALL" || discoverFilter == "ARTICLES")) {
                            item { DiscoverSectionRow(title = "FEATURED ARTICLE", emoji = "⭐", onSeeAll = { onNavigateToArticles() }, neonAlpha = neonAlpha); Spacer(modifier = Modifier.height(10.dp)); FeaturedArticleCard(article = displayFeatured) }
                        }

                        // 📝 Recent Articles — Firestore only
                        val displayArticles = recentArticles.distinctBy { it.id }.take(8)
                        if (displayArticles.isNotEmpty() && (discoverFilter == "ALL" || discoverFilter == "ARTICLES")) {
                            item {
                                DiscoverSectionRow(title = "RECENT ARTICLES", emoji = "📝", onSeeAll = { onNavigateToArticles() }, neonAlpha = neonAlpha)
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 2.dp)) { items(displayArticles, key = { "art_${it.id}" }) { article -> ArticleMiniCard(article = article) } }
                            }
                        }

                        // 📰 Magazines (console-filtered)
                        if (discoverFilter == "ALL" || discoverFilter == "MAGAZINES") {
                            item {
                                DiscoverSectionRow(title = "FEATURED MAGAZINES", emoji = "📰", onSeeAll = { onNavigateToMagazines() }, neonAlpha = neonAlpha)
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 2.dp)) { items(filteredMagazines.take(10), key = { "mag_${it.id}" }) { mag -> ScrapbookTrendingMagazineCard(magazine = mag) } }
                            }
                        }

                        // 🎵 Albums (console-filtered)
                        if (discoverFilter == "ALL" || discoverFilter == "ALBUMS") {
                            item {
                                DiscoverSectionRow(title = "FEATURED ALBUMS", emoji = "🎵", onSeeAll = { onNavigateToAlbums() }, neonAlpha = neonAlpha)
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 2.dp)) { items(filteredAlbums.take(10), key = { "alb_${it.id}" }) { album -> ScrapbookTrendingAlbumCard(album = album) } }
                            }
                        }
                    }
                }

                realUsers.isEmpty() && filteredLocal.isEmpty() && !isSearchingUsers -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No results for \"$searchQuery\"", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
                        }
                    }
                }

                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                        if (realUsers.isNotEmpty()) {
                            item {
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("USERS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = ScrapbookBorder.copy(alpha = 0.2f))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(CGreen).border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) { Text("${realUsers.size}", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp) }
                                }
                            }
                            itemsIndexed(realUsers, key = { _, u -> u.uid }) { index, user ->
                                SpringEntrance(delayMs = index * 50) {
                                    ScrapbookDiscoverUserCard(user = user, isFollowing = followingUids.contains(user.uid), isCurrentUser = user.uid == currentUser?.uid, onFollowClick = { toggleFollow(user) }, onTap = { selectedUser = user })
                                }
                            }
                        }
                        DiscoverCategory.values().filter { it != DiscoverCategory.USER }.forEach { category ->
                            val categoryResults = groupedLocal[category]
                            if (!categoryResults.isNullOrEmpty()) {
                                item {
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(category.label, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        HorizontalDivider(modifier = Modifier.weight(1f), color = ScrapbookBorder.copy(alpha = 0.2f))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(CGreen).border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) { Text("${categoryResults.size}", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp) }
                                    }
                                }
                                itemsIndexed(categoryResults, key = { _, r -> r.id }) { index, result ->
                                    SpringEntrance(delayMs = index * 50) { ScrapbookDiscoverResultCard(result = result) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Browse by Console Chips ─────────────────────────────────────────────────

@Composable
fun BrowseByConsolePills(selectedConsole: String, onConsoleSelected: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().background(ComicGlassBg).padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(consoleFilterChips) { jumpIndex, chip ->
            Box(modifier = Modifier.jumpIn(jumpIndex)) {
            val key = if (chip.startsWith("🕹️")) "ALL" else chip
            val isSelected = selectedConsole == key
            var pressed by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(targetValue = if (pressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "consolePillScale")
            Box(
                modifier = Modifier.scale(scale)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) ScrapbookDark else ComicGlassBg)
                    .border(2.5.dp, ScrapbookDark, RoundedCornerShape(20.dp))
                    .clickable { pressed = true; onConsoleSelected(key) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(chip, fontFamily = BangersFontFamily, color = if (isSelected) CGreen else ScrapbookDark, fontSize = 12.sp)
            }
            LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
                    }
        }
    }
}

// ─── Search History Chips ─────────────────────────────────────────────────────

@Composable
fun SearchHistoryChips(recentSearches: List<String>, onChipClick: (String) -> Unit, onRemove: (String) -> Unit) {
    if (recentSearches.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text("RECENT", fontFamily = BangersFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, letterSpacing = 1.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(recentSearches) { term ->
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .background(CGreen.copy(alpha = 0.14f))
                        .border(1.dp, CGreen.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                        .clickable { onChipClick(term) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = ScrapbookTextMuted, modifier = Modifier.size(12.dp))
                        Text(term, fontFamily = NunitoFontFamily, color = ScrapbookDark, fontSize = 12.sp)
                        Box(modifier = Modifier.clickable { onRemove(term) }) {
                            Icon(Icons.Filled.Close, contentDescription = null, tint = ScrapbookTextMuted, modifier = Modifier.size(11.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─── Picked For You ──────────────────────────────────────────────────────────

private fun articleCategory(title: String): Pair<String, Color> {
    val t = title.lowercase()
    return when {
        t.contains("jrpg") || t.contains("rpg") || t.contains("fantasy") -> "⚔️ RPG" to CAcPurple
        t.contains("mario") || t.contains("nintendo") || t.contains("zelda") -> "🍄 NINTENDO" to CAcRed
        t.contains("sonic") || t.contains("sega") -> "💨 SEGA" to CGreenDeep
        t.contains("ps1") || t.contains("playstation") || t.contains("polygon") -> "💿 PLAYSTATION" to CAcBlue
        t.contains("game boy") || t.contains("handheld") || t.contains("gb") -> "🎮 HANDHELD" to CGreenDeep
        t.contains("speedrun") -> "⚡ SPEEDRUN" to Color(0xFFE65100)
        t.contains("music") || t.contains("kondo") || t.contains("soundtrack") || t.contains("composer") -> "🎵 MUSIC" to Color(0xFF1A237E)
        t.contains("history") || t.contains("famicom") || t.contains("golden age") || t.contains("legacy") -> "📜 HISTORY" to Color(0xFF4E342E)
        t.contains("hidden gem") || t.contains("secret") || t.contains("missed") -> "💎 HIDDEN GEM" to CGreenDeep
        else -> "📰 FEATURE" to Color(0xFF37474F)
    }
}

@Composable
fun PickedForYouSection(currentUserUid: String?, articles: List<ArticleItem>, neonAlpha: Float) {
    val picks = remember(currentUserUid, articles) {
        val seed = (System.currentTimeMillis() / 86_400_000L + (currentUserUid?.hashCode()?.toLong() ?: 0L)).toInt()
        articles.shuffled(java.util.Random(seed.toLong())).take(3)
    }
    if (picks.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DiscoverSectionRow(title = "PICKED FOR YOU", emoji = "⭐", onSeeAll = null, neonAlpha = neonAlpha)
        picks.forEach { article -> PickedForYouCard(article = article) }
    }
}

// ─── Featured Story Hero Card ─────────────────────────────────────────────────

@Composable
fun FeaturedStoryHeroCard(article: ArticleItem, neonAlpha: Float) {
    val (categoryLabel, categoryColor) = remember(article.title) { articleCategory(article.title) }
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.98f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "heroScale")
    Box(modifier = Modifier.fillMaxWidth().scale(cardScale)) {
        // Comic shadow
        Box(
            modifier = Modifier.matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(ScrapbookDark.copy(alpha = 0.12f))
        )
        // Glow shadow
        Box(modifier = Modifier.matchParentSize().padding(6.dp).clip(RoundedCornerShape(20.dp))
            .background(CGreen.copy(neonAlpha * 0.18f)))
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(ScrapbookDark)
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(20.dp))
                .clickable { pressed = true }
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                if (!article.imageUrl.isNullOrBlank()) {
                    AsyncImage(model = article.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(article.imageUrl).fillMaxSize(), alpha = 0.45f)
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(listOf(CGreenMint, CGreenDeep))))
                    // Decorative circles
                    Box(modifier = Modifier.size(180.dp).offset(x = 160.dp, y = (-30).dp).clip(CircleShape).background(categoryColor.copy(0.08f)))
                    Box(modifier = Modifier.size(100.dp).offset(x = (-20).dp, y = 140.dp).clip(CircleShape).background(CGreen.copy(0.05f)))
                }
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(0.08f), Color.Black.copy(0.82f)))))
                Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(CGreen).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("✨ FEATURED", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 10.sp, letterSpacing = 0.5.sp)
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(categoryColor).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text(categoryLabel, fontFamily = BangersFontFamily, color = Color.White, fontSize = 10.sp)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(article.title, fontFamily = BangersFontFamily, color = Color.White, fontSize = 22.sp, lineHeight = 26.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, letterSpacing = 0.2.sp)
                        if (article.snippet.isNotBlank()) {
                            Text(article.snippet, fontFamily = NunitoFontFamily, color = Color.White.copy(0.65f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 17.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            if (article.author.isNotBlank()) {
                                Text("by ${article.author}", fontFamily = NunitoFontFamily, color = Color.White.copy(0.45f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(CGreen).padding(horizontal = 14.dp, vertical = 7.dp)) {
                                Text("READ NOW →", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp, letterSpacing = 0.5.sp)
                            }
                        }
                    }
                }
                ScanlineOverlay(Modifier.matchParentSize())
                ComicShimmer(Modifier.matchParentSize(), cornerRadius = 20.dp)
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

@Composable
private fun PickedForYouCard(article: ArticleItem) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "pfyCardScale")
    val (categoryLabel, categoryColor) = remember(article.title) { articleCategory(article.title) }
    val readMinutes = remember(article.fullContent) { (article.fullContent.length / 1000).coerceAtLeast(2) }
    Box(modifier = Modifier.fillMaxWidth().scale(scale)) {
        Box(modifier = Modifier.matchParentSize().offset(x = 4.dp, y = 5.dp).clip(RoundedCornerShape(14.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFFAF3E8), Color(0xFFF2E8CE))))
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))
                .clickable { pressed = true }
        ) {
            // Left accent color bar
            Box(modifier = Modifier.width(5.dp).fillMaxHeight().background(
                Brush.verticalGradient(listOf(categoryColor, categoryColor.copy(0.55f)))
            ).align(Alignment.CenterStart))
            Column(modifier = Modifier.padding(start = 19.dp, end = 14.dp, top = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Category badge + read time
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(categoryColor).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text(categoryLabel, fontFamily = BangersFontFamily, color = Color.White, fontSize = 9.sp, letterSpacing = 0.5.sp)
                    }
                    Text("$readMinutes min read", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
                }
                // Title
                Text(article.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp, letterSpacing = 0.2.sp)
                // Description excerpt
                if (article.snippet.isNotBlank()) {
                    Text(article.snippet, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 17.sp)
                }
                // Author + Read button row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("by ${article.author}", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted.copy(0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookDark).padding(horizontal = 10.dp, vertical = 5.dp)) {
                        Text("READ →", fontFamily = BangersFontFamily, color = CGreen, fontSize = 11.sp, letterSpacing = 0.5.sp)
                    }
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Friends Are Playing ──────────────────────────────────────────────────────

@Composable
fun FriendsArePlayingSection(displayData: List<Triple<String, String, String>>, neonAlpha: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DiscoverSectionRow(title = "FRIENDS ARE PLAYING", emoji = "🕹️", onSeeAll = null, neonAlpha = neonAlpha)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
            items(displayData, key = { it.first }) { (username, avatarUrl, gameName) ->
                FriendPlayingCard(username = username, avatarUrl = avatarUrl, gameName = gameName)
            }
        }
    }
}

@Composable
private fun FriendPlayingCard(username: String, avatarUrl: String, gameName: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.width(76.dp)) {
        Box(modifier = Modifier.size(60.dp).clip(CircleShape)
            .background(Brush.radialGradient(listOf(CGreen.copy(0.25f), ScrapbookDark.copy(0.06f))))
            .border(2.dp, Brush.linearGradient(listOf(CGreen, CGreen.copy(0.5f))), CircleShape)
        ) {
            if (avatarUrl.isNotBlank()) {
                AsyncImage(model = avatarUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(avatarUrl).fillMaxSize())
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(username.firstOrNull()?.uppercase() ?: "?", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp)
                }
            }
            Box(modifier = Modifier.size(13.dp).clip(CircleShape).background(Color(0xFF22CC66)).border(2.dp, ComicGlassBg, CircleShape).align(Alignment.BottomEnd))
        }
        Text(username.take(10), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookDark.copy(0.07f)).border(1.dp, ScrapbookBorder.copy(0.3f), RoundedCornerShape(6.dp)).padding(horizontal = 5.dp, vertical = 3.dp)) {
            Text(gameName.take(14), fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

// ─── This Week in Gaming History ─────────────────────────────────────────────

@Composable
fun GamingHistorySection(liveEvents: List<GamingHistoryEvent> = emptyList(), neonAlpha: Float) {
    val currentMonth = remember { java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1 }
    val events = remember(liveEvents, currentMonth) {
        val source = liveEvents.ifEmpty {
            val same = gamingHistoryEvents.filter { it.month == currentMonth }
            val near = gamingHistoryEvents.filter { it.month == currentMonth - 1 || it.month == currentMonth + 1 }
            (same + near).ifEmpty { gamingHistoryEvents }
        }
        source.take(6)
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DiscoverSectionRow(title = "THIS WEEK IN HISTORY", emoji = "🗓️", onSeeAll = null, neonAlpha = neonAlpha)
        // Timeline strip
        Box(modifier = Modifier.fillMaxWidth().padding(start = 2.dp, end = 2.dp)) {
            // Horizontal timeline line — sits 26dp from top (center of year badge)
            Box(
                modifier = Modifier.fillMaxWidth().height(2.dp).offset(y = 26.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, CGreen.copy(0.6f), CGreen.copy(0.6f), Color.Transparent)))
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
                items(events, key = { "${it.year}_${it.title}" }) { event ->
                    HistoryEventCard(event = event)
                }
            }
        }
    }
}

@Composable
private fun HistoryEventCard(event: GamingHistoryEvent) {
    val accentColors = listOf(CAcPurple, CAcRed, CAcBlue, CGreenDeep, CAcYellow, Color(0xFF996600))
    val accent = accentColors[Math.abs(event.title.hashCode()) % accentColors.size]
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "histScale")

    Column(
        modifier = Modifier.width(175.dp).scale(cardScale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Year stamp badge (sits on timeline line)
        Box(
            modifier = Modifier.clip(RoundedCornerShape(10.dp))
                .background(accent)
                .border(2.dp, ScrapbookDark, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                event.year.toString(),
                fontFamily = BangersFontFamily,
                color = Color.White,
                fontSize = 15.sp,
                letterSpacing = 1.sp
            )
        }
        // Connector dot
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accent))
        Spacer(Modifier.height(4.dp))
        // Card body
        Box(modifier = Modifier.width(175.dp)) {
            Box(modifier = Modifier.matchParentSize().offset(x = 3.dp, y = 3.dp).clip(RoundedCornerShape(14.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
            Box(
                modifier = Modifier.width(175.dp).clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.92f))
                    .border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))
                    .clickable { pressed = true }
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(event.emoji, fontSize = 22.sp)
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(accent.copy(0.15f)).border(1.dp, accent.copy(0.3f), RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 3.dp)) {
                            Text(event.console, fontFamily = BangersFontFamily, color = accent, fontSize = 10.sp)
                        }
                    }
                    Text(event.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                    Text(event.description, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Active Right Now ─────────────────────────────────────────────────────────

@Composable
fun ActiveRightNowSection(items: List<CommunityPulseItem> = emptyList(), neonAlpha: Float) {
    val displayItems = items.ifEmpty { communityPulseItems }.take(4)
    val pulseT = rememberInfiniteTransition(label = "pulseGreen")
    val pulseAlpha by pulseT.animateFloat(initialValue = 0.35f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse), label = "pulseA")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Section header with pulsing LIVE badge
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(4.dp).height(26.dp).clip(RoundedCornerShape(2.dp))
                .background(CGreen.copy(alpha = pulseAlpha)))
            Spacer(Modifier.width(8.dp))
            Text("🟢", fontSize = 20.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                "ACTIVE RIGHT NOW",
                fontFamily = BangersFontFamily,
                color = ScrapbookDark,
                fontSize = 22.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CAcRed)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = pulseAlpha)))
                    Text("LIVE", fontFamily = BangersFontFamily, color = Color.White, fontSize = 11.sp, letterSpacing = 0.5.sp)
                }
            }
        }
        displayItems.forEach { item -> CommunityPulseCard(item = item, pulseAlpha = pulseAlpha) }
    }
}

@Composable
private fun CommunityPulseCard(item: CommunityPulseItem, pulseAlpha: Float) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.matchParentSize().offset(x = 3.dp, y = 3.dp).clip(RoundedCornerShape(12.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF22CC66).copy(alpha = pulseAlpha)))
                Text(item.emoji, fontSize = 20.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.topic, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp)
                    Text("${item.newPosts} new posts · ${item.minutesAgo}m ago", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(item.color).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("JOIN →", fontFamily = BangersFontFamily, color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}

// ─── Your Retro Recap ────────────────────────────────────────────────────────

@Composable
fun RetroRecapCard(displayName: String?, neonAlpha: Float) {
    val dayName = remember { java.util.Calendar.getInstance().getDisplayName(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SHORT, java.util.Locale.US) ?: "Today" }
    val seed = remember { (displayName?.hashCode() ?: 0) + java.util.Calendar.getInstance().get(java.util.Calendar.WEEK_OF_YEAR) }
    val rng = remember(seed) { java.util.Random(seed.toLong()) }
    val stats = remember(seed) {
        listOf(
            Triple("🎮", "Genres explored", "${2 + rng.nextInt(5)}"),
            Triple("🏁", "Checkpoints", "${rng.nextInt(4)}"),
            Triple("📰", "Articles read", "${1 + rng.nextInt(5)}"),
            Triple("👥", "New follows", "${rng.nextInt(5)}")
        )
    }
    val stampGlow by rememberGlowRange(0.5f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DiscoverSectionRow(title = "YOUR RETRO RECAP", emoji = "📊", onSeeAll = null, neonAlpha = neonAlpha)
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.matchParentSize().offset(x = 5.dp, y = 5.dp).clip(RoundedCornerShape(16.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(ComicGlassBg, Color(0xFFF5E8C0))))
                    .border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("THIS WEEK", fontFamily = BangersFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, letterSpacing = 1.sp)
                            Text("${(displayName?.uppercase() ?: "EXPLORER")}'S STATS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 17.sp)
                        }
                        Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(CGreen.copy(stampGlow * 0.14f)).border(2.dp, Brush.linearGradient(listOf(CGreen.copy(stampGlow), ScrapbookDark.copy(0.25f))), CircleShape), contentAlignment = Alignment.Center) {
                            Text("🏅", fontSize = 22.sp)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        stats.take(2).forEach { (emoji, label, value) -> RecapStatBox(emoji, label, value, modifier = Modifier.weight(1f)) }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        stats.drop(2).forEach { (emoji, label, value) -> RecapStatBox(emoji, label, value, modifier = Modifier.weight(1f)) }
                    }
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(CGreen.copy(0.11f)).border(1.dp, CGreen.copy(0.30f), RoundedCornerShape(8.dp)).padding(10.dp)) {
                        Text("Most active: $dayName · Keep exploring retro history! 🕹️", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(0.75f), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun RecapStatBox(emoji: String, label: String, value: String, modifier: Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(10.dp)).background(ScrapbookDark.copy(0.05f)).border(1.dp, ScrapbookBorder.copy(0.3f), RoundedCornerShape(10.dp)).padding(10.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(emoji, fontSize = 18.sp)
            Text(value, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 24.sp)
            Text(label, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 14.sp)
        }
    }
}

// ─── Trending Hashtags with HOT / RISING Toggle ───────────────────────────────

@Composable
fun TrendingHashtagsSection(hotTags: List<TrendingTag> = emptyList(), risingTags: List<TrendingTag> = emptyList(), neonAlpha: Float) {
    var mode by remember { mutableStateOf("HOT") }
    val activeTags = if (mode == "HOT") hotTags.ifEmpty { hotTrendingTags } else risingTags.ifEmpty { risingTrendingTags }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(26.dp).clip(RoundedCornerShape(2.dp)).background(CGreen))
            Spacer(modifier = Modifier.width(8.dp))
            Text("🔥", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text("TRENDING", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
            Row(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookDark.copy(0.06f)).border(1.dp, ScrapbookBorder.copy(0.25f), RoundedCornerShape(8.dp))) {
                listOf("HOT" to "🔥", "RISING" to "📈").forEach { (m, icon) ->
                    val active = mode == m
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (active) CGreen else Color.Transparent).clickable { mode = m }.padding(horizontal = 11.dp, vertical = 6.dp)) {
                        Text("$icon $m", fontFamily = BangersFontFamily, color = if (active) ScrapbookDark else ScrapbookTextMuted, fontSize = 11.sp)
                    }
                }
            }
        }
        activeTags.forEach { tag -> TrendingTagCard(tag = tag) }
    }
}

@Composable
private fun TrendingTagCard(tag: TrendingTag) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "tagCardScale")
    val dotColor = if (tag.isHot) Color(0xFFFF6600) else Color(0xFF22CC66)
    val growthColor = if (tag.isHot) Color(0xFFCC4400) else Color(0xFF1A8A1A)
    val growthBg = if (tag.isHot) Color(0xFFFF6600).copy(0.11f) else Color(0xFF22CC66).copy(0.11f)

    Box(modifier = Modifier.fillMaxWidth().scale(scale)) {
        Box(modifier = Modifier.matchParentSize().offset(x = 3.dp, y = 3.dp).clip(RoundedCornerShape(12.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(12.dp))
                .clickable { pressed = true }
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(tag.tag, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp)
                        Text(tag.context, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(growthBg).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text(tag.growth, fontFamily = BangersFontFamily, color = growthColor, fontSize = 10.sp)
                        }
                        Text("${tag.count} posts", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
                    }
                }
                // Animated growth bar
                val barFraction = (tag.count.toFloat() / 3000f).coerceIn(0.05f, 1f)
                var barVisible by remember { mutableStateOf(false) }
                val animatedBar by animateFloatAsState(
                    targetValue = if (barVisible) barFraction else 0f,
                    animationSpec = tween(700, easing = FastOutSlowInEasing),
                    label = "growthBar_${tag.tag}"
                )
                LaunchedEffect(Unit) { delay(100); barVisible = true }
                Box(modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookDark.copy(0.08f))) {
                    Box(modifier = Modifier.fillMaxWidth(animatedBar).height(3.dp).clip(RoundedCornerShape(2.dp)).background(
                        Brush.horizontalGradient(listOf(dotColor, dotColor.copy(0.5f)))
                    ))
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Section Row ──────────────────────────────────────────────────────────────

@Composable
fun DiscoverSectionRow(title: String, emoji: String, onSeeAll: (() -> Unit)?, neonAlpha: Float = 0.6f) {
    val glowAlpha by rememberGlowPhase(0.45f)
    var headerPressed by remember { mutableStateOf(false) }
    val pressAnim by animateFloatAsState(targetValue = if (headerPressed) 4f else 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "rshPress")
    val shadowOffset by animateFloatAsState(targetValue = if (headerPressed) 0f else 4f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "rshShadow")

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        // Outer glow layer
        Box(modifier = Modifier.matchParentSize().offset(x = 7.dp, y = 7.dp).clip(RoundedCornerShape(8.dp)).background(CGreen.copy(alpha = if (headerPressed) 0f else glowAlpha * 0.28f)))
        // Hard shadow
        Box(modifier = Modifier.matchParentSize().offset(x = shadowOffset.dp, y = shadowOffset.dp).clip(RoundedCornerShape(8.dp)).background(CGreen.copy(alpha = if (headerPressed) 0.3f else 1f)))
        // Glass panel
        Box(modifier = Modifier.fillMaxWidth().offset(x = pressAnim.dp, y = pressAnim.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.5.dp, ScrapbookDark, RoundedCornerShape(8.dp)).clickable { headerPressed = true }.padding(horizontal = 14.dp, vertical = 9.dp)) {
            // Halftone dots canvas
            HalftoneDots(Modifier.matchParentSize(), spacing = 6.dp, dotRadius = 1.3.dp, color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.13f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = emoji, fontSize = 18.sp)
                Text(text = title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                if (onSeeAll != null) {
                    var seeAllPressed by remember { mutableStateOf(false) }
                    val seeAllOff by animateFloatAsState(targetValue = if (seeAllPressed) 0f else 2f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "seeAllOff")
                    Box(modifier = Modifier.offset(x = (-seeAllOff).dp, y = (-seeAllOff).dp).clip(RoundedCornerShape(6.dp)).background(CGreen).border(1.5.dp, ScrapbookDark, RoundedCornerShape(6.dp)).clickable { seeAllPressed = true; onSeeAll() }.padding(horizontal = 10.dp, vertical = 5.dp)) {
                        Text("SEE ALL →", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp)
                    }
                    LaunchedEffect(seeAllPressed) { if (seeAllPressed) { delay(150); seeAllPressed = false } }
                }
            }
        }
    }
    LaunchedEffect(headerPressed) { if (headerPressed) { delay(150); headerPressed = false } }
}

// ─── Featured Article Card ────────────────────────────────────────────────────

@Composable
fun FeaturedArticleCard(article: ArticleItem) {
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "featuredScale")
    Box(modifier = Modifier.scale(cardScale).clickable { pressed = true }) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ComicGlassBg, cornerRadius = 14.dp, shadowOffset = 5.dp) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))) {
                    if (!article.imageUrl.isNullOrBlank()) {
                        AsyncImage(model = article.imageUrl, contentDescription = article.title, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(article.imageUrl).fillMaxSize())
                    } else { Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) { Text("📰", fontSize = 48.sp) } }
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))))
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(10.dp).clip(RoundedCornerShape(6.dp)).background(CGreen).border(2.dp, ScrapbookDark.copy(alpha = 0.3f), RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) { Text("⭐ COMMUNITY PICK", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp) }
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                        Text(article.title, fontFamily = BangersFontFamily, color = Color.White, fontSize = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 24.sp)
                        if (article.author.isNotBlank()) Text("by ${article.author}", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
                if (article.snippet.isNotBlank()) { Text(article.snippet, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp, modifier = Modifier.padding(12.dp)) }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Article Mini Card ────────────────────────────────────────────────────────

@Composable
fun ArticleMiniCard(article: ArticleItem) {
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.95f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "miniScale")
    Box(modifier = Modifier.width(190.dp).scale(cardScale).clickable { pressed = true }) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ComicGlassBg, cornerRadius = 12.dp, shadowOffset = 3.dp) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(Color.White.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
                    if (!article.imageUrl.isNullOrBlank()) { AsyncImage(model = article.imageUrl, contentDescription = article.title, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(article.imageUrl).fillMaxSize()) }
                    else { Text("📝", fontSize = 32.sp) }
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f)))))
                }
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(article.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 17.sp)
                    if (article.author.isNotBlank()) { Spacer(modifier = Modifier.height(3.dp)); Text("by ${article.author}", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Live Streamer Card ───────────────────────────────────────────────────────

@Composable
fun LiveStreamerCard(streamer: CommunityStreamer) {
    val liveT = rememberInfiniteTransition(label = "live_${streamer.uid}")
    val livePulse by liveT.animateFloat(initialValue = 0.5f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(700, easing = EaseInOut), RepeatMode.Reverse), label = "livePulse")

    Box(modifier = Modifier.width(120.dp)) {
        // ✅ Red neon glow on live card
        Box(modifier = Modifier.matchParentSize().padding(3.dp).blur(10.dp).background(Color.Red.copy(alpha = livePulse * 0.3f), RoundedCornerShape(12.dp)))
        ScrapbookCard(modifier = Modifier.fillMaxWidth().border(1.5.dp, Color.Red.copy(alpha = livePulse * 0.8f), RoundedCornerShape(12.dp)), backgroundColor = ComicGlassBg, cornerRadius = 12.dp, shadowOffset = 3.dp) {
            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box {
                    Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)).border(3.dp, Color.Red.copy(alpha = livePulse), CircleShape), contentAlignment = Alignment.Center) {
                        if (streamer.profilePicUrl.isNotBlank()) { AsyncImage(model = streamer.profilePicUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(streamer.profilePicUrl).fillMaxSize()) }
                        else { Text(streamer.username.take(1).uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 24.sp) }
                    }
                    Box(modifier = Modifier.align(Alignment.TopEnd).size(14.dp).clip(CircleShape).background(Color.Red.copy(alpha = livePulse)).border(2.dp, Color.White, CircleShape))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(streamer.username, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color.Red).padding(horizontal = 8.dp, vertical = 3.dp)) { Text("🔴 LIVE", fontFamily = BangersFontFamily, color = Color.White, fontSize = 11.sp) }
                if (streamer.twitchUsername.isNotBlank()) { Spacer(modifier = Modifier.height(4.dp)); Text("/${streamer.twitchUsername}", fontFamily = NunitoFontFamily, color = CAcPurple, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            }
        }
    }
}

// ─── Trending Players Card ────────────────────────────────────────────────────

@Composable
fun TrendingPlayersCard(users: List<UserProfileData>, followingUids: Set<String>, currentUid: String, onFollowClick: (UserProfileData) -> Unit, onTap: (UserProfileData) -> Unit) {
    Box {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ComicGlassBg, cornerRadius = 14.dp, shadowOffset = 4.dp) {
            Column(modifier = Modifier.fillMaxWidth()) {
                users.forEachIndexed { index, user ->
                    var pressed by remember { mutableStateOf(false) }
                    val rowScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "playerRow_$index")
                    Row(modifier = Modifier.fillMaxWidth().scale(rowScale).clickable { pressed = true; onTap(user) }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        // ✅ Gold/silver/bronze medals with neon glow
                        val medalColor = when (index) { 0 -> Color(0xFFFFD700); 1 -> Color(0xFFC0C0C0); else -> Color(0xFFCD7F32) }
                        Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(medalColor).border(2.dp, medalColor.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
                            Text("${index + 1}", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookBorder, CircleShape), contentAlignment = Alignment.Center) {
                            if (!user.profilePictureUrl.isNullOrBlank()) { AsyncImage(model = user.profilePictureUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(user.profilePictureUrl).fillMaxSize()) }
                            else { Text(user.username.take(1).uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp) }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.username.uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${formatCount(user.followersCount)} followers", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                        }
                        if (user.uid != currentUid) {
                            val isFollowing = followingUids.contains(user.uid)
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(if (isFollowing) Color.White.copy(alpha = 0.46f) else CGreen)
                                    .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                                    .clickable { onFollowClick(user) }.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(if (isFollowing) "FOLLOWING" else "FOLLOW", fontFamily = BangersFontFamily, fontSize = 12.sp, color = ScrapbookDark)
                            }
                        }
                    }
                    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
                    if (index < users.size - 1) HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = ScrapbookBorder.copy(alpha = 0.15f))
                }
            }
        }
    }
}

// ─── Trending User Card ───────────────────────────────────────────────────────

@Composable
fun ScrapbookTrendingUserCard(user: UserProfileData, isFollowing: Boolean, onTap: () -> Unit, onFollowClick: () -> Unit) {
    val glowAlpha by rememberGlowPhase(0.4f)
    var pressed by remember { mutableStateOf(false) }
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "trendPress")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "trendShadow")
    val tiltDeg = remember(user.uid) { ((user.uid.hashCode() % 5) - 2) * 0.5f }
    Box(modifier = Modifier.width(110.dp).graphicsLayer { rotationZ = tiltDeg }) {
        Box(modifier = Modifier.matchParentSize().offset(5.dp, 5.dp).clip(RoundedCornerShape(12.dp)).background(CGreen.copy(alpha = glowAlpha * 0.25f)))
        Box(modifier = Modifier.matchParentSize().offset(shadowOff.dp, shadowOff.dp).clip(RoundedCornerShape(12.dp)).background(CGreen))
        Box(modifier = Modifier.fillMaxWidth().offset(y = pressAnim.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.5.dp, ScrapbookDark, RoundedCornerShape(12.dp)).clickable { pressed = true; onTap() }) {
            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookBorder, CircleShape), contentAlignment = Alignment.Center) {
                    if (!user.profilePictureUrl.isNullOrBlank()) { AsyncImage(model = user.profilePictureUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(user.profilePictureUrl).fillMaxSize()) }
                    else { Icon(Icons.Filled.Person, contentDescription = null, tint = ScrapbookTextMuted, modifier = Modifier.size(28.dp)) }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(user.username, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text("${formatCount(user.followersCount)} followers", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                RetroGlassButton(
                    text = if (isFollowing) "FOLLOWING" else "FOLLOW",
                    onClick = { onFollowClick() },
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 8.dp,
                    burstText = if (isFollowing) null else "POW!"
                )
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Magazine Card ────────────────────────────────────────────────────────────

private fun eraPlateColor(era: String): Color = when (era.uppercase()) {
    "PS1" -> CAcBlue
    "PS2" -> Color(0xFF00287A)
    "SNES" -> CAcPurple
    "NES" -> CAcRedD
    "N64" -> Color(0xFF1A237E)
    "SEGA" -> CGreenDeep
    "GBA" -> CAcPurple
    "ARCADE" -> Color(0xFF311B92)
    "PC" -> Color(0xFF1B5E20)
    "GB" -> CGreenDeep
    "GCN" -> Color(0xFF4527A0)
    else -> Color(0xFF37474F)
}

@Composable
fun ScrapbookTrendingMagazineCard(magazine: MagazineCover) {
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "magCardScale")
    val tiltY by animateFloatAsState(targetValue = if (pressed) -10f else 0f, animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium), label = "magTiltY")
    val plateColor = remember(magazine.era) { eraPlateColor(magazine.era) }
    Box(modifier = Modifier.width(138.dp).scale(cardScale)
        .graphicsLayer { rotationY = tiltY; cameraDistance = 8f * density }
        .clickable { pressed = true }) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ComicGlassBg, cornerRadius = 12.dp, shadowOffset = 4.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Cover art area
                Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))) {
                    when {
                        magazine.coverImageResId != null -> Image(
                            painter = painterResource(id = magazine.coverImageResId),
                            contentDescription = magazine.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                        )
                        !magazine.coverImageUrl.isNullOrBlank() -> AsyncImage(
                            model = magazine.coverImageUrl, contentDescription = magazine.title,
                            contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(magazine.coverImageUrl).fillMaxSize()
                        )
                        else -> {
                            // Stylized era-colored placeholder
                            Box(modifier = Modifier.fillMaxSize().background(
                                Brush.verticalGradient(listOf(plateColor, plateColor.copy(alpha = 0.55f)))
                            )) {
                                // Grid lines for magazine feel
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter).offset(y = 40.dp).background(Color.White.copy(0.08f)))
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter).offset(y = 80.dp).background(Color.White.copy(0.08f)))
                                Column(modifier = Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("📰", fontSize = 22.sp)
                                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color.White.copy(0.18f)).border(0.5.dp, Color.White.copy(0.3f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                                            Text(magazine.era.take(6), fontFamily = BangersFontFamily, color = Color.White, fontSize = 9.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(magazine.title, fontFamily = BangersFontFamily, color = Color.White, fontSize = 12.sp, maxLines = 4, lineHeight = 15.sp, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                    // Overlay gradient
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)))))
                    // Platform badge top-right
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(5.dp)).background(plateColor.copy(0.9f)).border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(5.dp)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                        Text(magazine.platform.take(6), fontFamily = BangersFontFamily, color = Color.White, fontSize = 8.sp)
                    }
                }
                // Info strip
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(magazine.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(magazine.era, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 9.sp)
                        Text("READ →", fontFamily = BangersFontFamily, color = plateColor, fontSize = 9.sp)
                    }
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Album Card ───────────────────────────────────────────────────────────────

private fun vibeColor(vibe: String): Color = when (vibe.uppercase()) {
    "EPIC"      -> Color(0xFF7B1FA2)
    "HYPE"      -> Color(0xFFD84315)
    "CHILL"     -> CAcBlue
    "NOSTALGIC" -> CGreenDeep
    "DARK"      -> Color(0xFF37474F)
    else        -> Color(0xFF4E342E)
}

@Composable
fun ScrapbookTrendingAlbumCard(album: Album) {
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "albumCardScale")
    val vColor = remember(album.vibe) { vibeColor(album.vibe ?: "NOSTALGIC") }
    // Vinyl spin — always spinning slowly, speeds up on press
    val vinylT = rememberInfiniteTransition(label = "vinylSpin_${album.id}")
    val vinylAngle by vinylT.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Restart),
        label = "vinylAngle_${album.id}"
    )
    Box(modifier = Modifier.width(138.dp).scale(cardScale).clickable { pressed = true }) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ComicGlassBg, cornerRadius = 12.dp, shadowOffset = 4.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Art / vinyl area
                Box(modifier = Modifier.fillMaxWidth().height(138.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(ScrapbookDark)) {
                    if (album.coverImageResId != null) {
                        Image(painter = painterResource(id = album.coverImageResId), contentDescription = album.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().rotate(vinylAngle))
                    } else {
                        // Vinyl record concentric circles — spinning
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.size(112.dp).rotate(vinylAngle).clip(CircleShape).background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.44f), ScrapbookDark))))
                            Box(modifier = Modifier.size(88.dp).rotate(vinylAngle).clip(CircleShape).background(ScrapbookDark).border(0.5.dp, Color(0xFF333333), CircleShape))
                            Box(modifier = Modifier.size(66.dp).rotate(vinylAngle).clip(CircleShape).background(ScrapbookDark).border(0.5.dp, Color.White.copy(alpha = 0.44f), CircleShape))
                            // Coloured label in centre — counter-rotates slightly so text stays readable
                            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Brush.radialGradient(listOf(vColor, vColor.copy(0.6f)))), contentAlignment = Alignment.Center) {
                                Text(album.era.take(4), fontFamily = BangersFontFamily, color = Color.White, fontSize = 8.sp, textAlign = TextAlign.Center)
                            }
                            Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(ScrapbookDark))
                        }
                    }
                    // Vibe badge
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(5.dp)).background(vColor).border(0.5.dp, Color.White.copy(0.15f), RoundedCornerShape(5.dp)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                        Text(album.vibe?.take(9) ?: "RETRO", fontFamily = BangersFontFamily, color = Color.White, fontSize = 8.sp)
                    }
                    // Play count
                    Box(modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).clip(RoundedCornerShape(5.dp)).background(Color.Black.copy(0.55f)).padding(horizontal = 5.dp, vertical = 3.dp)) {
                        Text("▶ ${album.playCount}", fontFamily = NunitoFontFamily, color = Color.White, fontSize = 8.sp)
                    }
                }
                // Info strip
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(album.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
                    Text(album.artist, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("${album.era} · ${album.year}", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted.copy(0.55f), fontSize = 8.sp)
                        Text("PLAY →", fontFamily = BangersFontFamily, color = vColor, fontSize = 9.sp)
                    }
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── User Search Card ─────────────────────────────────────────────────────────

@Composable
fun ScrapbookDiscoverUserCard(user: UserProfileData, isFollowing: Boolean, isCurrentUser: Boolean, onFollowClick: () -> Unit, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val glowAlpha by rememberGlowPhase(0.4f)
    var pressed by remember { mutableStateOf(false) }
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "discPress")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "discShadow")
    val tiltDeg = remember(user.uid) { ((user.uid.hashCode() % 5) - 2) * 0.5f }
    Box(modifier = modifier.padding(vertical = 4.dp).graphicsLayer { rotationZ = tiltDeg }) {
        Box(modifier = Modifier.matchParentSize().offset(6.dp, 6.dp).clip(RoundedCornerShape(12.dp)).background(CGreen.copy(alpha = glowAlpha * 0.25f)))
        Box(modifier = Modifier.matchParentSize().offset(shadowOff.dp, shadowOff.dp).clip(RoundedCornerShape(12.dp)).background(CGreen))
        Box(modifier = Modifier.fillMaxWidth().offset(y = pressAnim.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.5.dp, ScrapbookDark, RoundedCornerShape(12.dp)).clickable { pressed = true; onTap() }) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookBorder, CircleShape), contentAlignment = Alignment.Center) {
                    if (!user.profilePictureUrl.isNullOrBlank()) { AsyncImage(model = user.profilePictureUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(user.profilePictureUrl).fillMaxSize()) }
                    else { Icon(Icons.Filled.Person, contentDescription = null, tint = ScrapbookTextMuted, modifier = Modifier.size(24.dp)) }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.username.uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(user.userHandle, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                    if (user.bio.isNotBlank()) Text(user.bio, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (!isCurrentUser) {
                    Spacer(modifier = Modifier.width(8.dp))
                    RetroGlassButton(
                        text = if (isFollowing) "FOLLOWING" else "FOLLOW",
                        onClick = { onFollowClick() },
                        cornerRadius = 8.dp,
                        burstText = if (isFollowing) null else "POW!"
                    )
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Result Card ──────────────────────────────────────────────────────────────

@Composable
fun ScrapbookDiscoverResultCard(result: DiscoverResult, modifier: Modifier = Modifier) {
    val glowAlpha by rememberGlowPhase(0.4f)
    var pressed by remember { mutableStateOf(false) }
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "resultPress")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "resultShadow")
    Box(modifier = modifier.padding(vertical = 4.dp)) {
        Box(modifier = Modifier.matchParentSize().offset(6.dp, 6.dp).clip(RoundedCornerShape(10.dp)).background(CGreen.copy(alpha = glowAlpha * 0.25f)))
        Box(modifier = Modifier.matchParentSize().offset(shadowOff.dp, shadowOff.dp).clip(RoundedCornerShape(10.dp)).background(CGreen))
        Box(modifier = Modifier.fillMaxWidth().offset(y = pressAnim.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.5.dp, ScrapbookDark, RoundedCornerShape(10.dp)).clickable { pressed = true }) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(CGreen, CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(result.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(result.subtitle, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookDark).border(1.dp, CGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text(result.category.label, fontFamily = BangersFontFamily, color = CGreen, fontSize = 11.sp)
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Backward Compat Wrappers ─────────────────────────────────────────────────

@Composable fun ScrapbookDiscoverHeader(title: String, emoji: String) { DiscoverSectionRow(title = title, emoji = emoji, onSeeAll = null) }
@Composable fun TrendingSectionHeader(title: String, color: Color) { DiscoverSectionRow(title = title, emoji = "🔥", onSeeAll = null) }
@Composable fun TrendingUserCard(user: UserProfileData, isFollowing: Boolean, onTap: () -> Unit, onFollowClick: () -> Unit) { ScrapbookTrendingUserCard(user = user, isFollowing = isFollowing, onTap = onTap, onFollowClick = onFollowClick) }
@Composable fun TrendingMagazineCard(magazine: MagazineCover) { ScrapbookTrendingMagazineCard(magazine = magazine) }
@Composable fun TrendingAlbumCard(album: Album) { ScrapbookTrendingAlbumCard(album = album) }
@Composable fun DiscoverUserCard(user: UserProfileData, isFollowing: Boolean, isCurrentUser: Boolean, onFollowClick: () -> Unit, onTap: () -> Unit, modifier: Modifier = Modifier) { ScrapbookDiscoverUserCard(user = user, isFollowing = isFollowing, isCurrentUser = isCurrentUser, onFollowClick = onFollowClick, onTap = onTap, modifier = modifier) }
@Composable fun DiscoverResultCard(result: DiscoverResult, modifier: Modifier = Modifier) { ScrapbookDiscoverResultCard(result = result, modifier = modifier) }
@Composable fun TrendingArticleCard(article: ArticleItem) { ScrapbookTrendingArticleCard(article = article) }
@Composable fun ScrapbookTrendingArticleCard(article: ArticleItem) {
    Box(modifier = Modifier.padding(vertical = 4.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ComicGlassBg, cornerRadius = 10.dp, shadowOffset = 3.dp) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    if (!article.imageUrl.isNullOrBlank()) { AsyncImage(model = article.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(article.imageUrl).fillMaxSize()) }
                    else { Text("📝", fontSize = 24.sp) }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(article.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp)
                    if (article.author.isNotBlank()) Text("by ${article.author}", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
                }
            }
        }
    }
}
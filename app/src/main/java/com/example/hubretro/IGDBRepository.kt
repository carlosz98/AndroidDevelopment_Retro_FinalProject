package com.example.hubretro

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class IGDBGame(
    val id: Int,
    val name: String,
    val coverUrl: String?,
    val rating: Double? = null,
    val releaseYear: Int? = null,
    val summary: String? = null,
    val platforms: List<String> = emptyList(),
    val genres: List<String> = emptyList(),          // e.g. ["Role-playing (RPG)", "Action"]
    val gameModes: List<String> = emptyList(),        // e.g. ["Single player", "Multiplayer"]
    val developer: String? = null,                   // first developer company name
    val esrbRating: String? = null,                  // "E", "E10+", "T", "M", "AO", "RP"
    val screenshots: List<String> = emptyList(),      // up to 4 screenshot URLs
    val videoIds: List<String> = emptyList()          // YouTube video IDs from IGDB videos field
)

data class IGDBSoundtrack(
    val id: Int,
    val name: String,
    val coverUrl: String?,
    val gameId: Int? = null,
    val gameName: String? = null
)

data class IGDBUpcomingGame(
    val id: Int,
    val name: String,
    val coverUrl: String?,
    val releaseDate: Long,           // Unix epoch in SECONDS (IGDB format)
    val summary: String?,
    val platforms: List<String>,
    val hypes: Int = 0,
    val category: Int = 0,           // 0=main game, 1=DLC, 2=expansion, 4=standalone expansion
    val genres: List<String> = emptyList(),
    val developer: String? = null,
    val esrbRating: String? = null,  // "E", "E10+", "T", "M", "AO", "RP"
    val gameModes: List<String> = emptyList(), // "Single player", "Multiplayer", "Co-operative"...
    val trailerVideoId: String? = null  // YouTube video ID from IGDB videos field
)

object IGDBRepository {

    private val CLIENT_ID     get() = BuildConfig.IGDB_CLIENT_ID
    private val CLIENT_SECRET get() = BuildConfig.IGDB_CLIENT_SECRET
    private const val BASE_URL = "https://api.igdb.com/v4"
    private const val TOKEN_URL = "https://id.twitch.tv/oauth2/token"
    private const val TAG = "IGDBRepository"

    // ✅ Always start with null so we always fetch fresh on first use
    @Volatile private var cachedToken: String? = null
    @Volatile private var tokenExpiresAt: Long = 0L

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // ✅ Force refresh on every app start by checking expiry properly
    private suspend fun getValidToken(): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val token = cachedToken
        if (token == null || now >= tokenExpiresAt - 60_000L) {
            Log.d(TAG, "Token missing or expiring — refreshing...")
            refreshToken()
        }
        cachedToken ?: throw Exception("Failed to obtain IGDB access token")
    }

    private suspend fun refreshToken() = withContext(Dispatchers.IO) {
        try {
            val body = FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .add("grant_type", "client_credentials")
                .build()

            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d(TAG, "Token refresh: ${response.code} — $responseBody")

            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "Token refresh failed: ${response.code}")
                cachedToken = null
                return@withContext
            }

            val json = JSONObject(responseBody)

            if (json.has("access_token")) {
                val newToken = json.getString("access_token")
                val expiresIn = json.optLong("expires_in", 3600L)
                cachedToken = newToken
                tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000L)
                Log.d(TAG, "✅ Token refreshed! Expires in ${expiresIn / 3600}h")
            } else {
                Log.e(TAG, "No access_token in response: $responseBody")
                cachedToken = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing token: ${e.message}", e)
            cachedToken = null
        }
    }

    // ✅ Shared helper for making IGDB requests with auto-retry on 401
    private suspend fun igdbRequest(
        endpoint: String,
        body: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val token = getValidToken()
            val request = Request.Builder()
                .url("$BASE_URL/$endpoint")
                .post(body.toRequestBody("text/plain".toMediaType()))
                .header("Client-ID", CLIENT_ID)
                .header("Authorization", "Bearer $token")
                .build()

            val response = client.newCall(request).execute()

            // 401 — force refresh and retry once
            if (response.code == 401) {
                Log.w(TAG, "401 on $endpoint — forcing token refresh and retry")
                cachedToken = null
                val freshToken = getValidToken()
                val retryRequest = Request.Builder()
                    .url("$BASE_URL/$endpoint")
                    .post(body.toRequestBody("text/plain".toMediaType()))
                    .header("Client-ID", CLIENT_ID)
                    .header("Authorization", "Bearer $freshToken")
                    .build()
                val retryResponse = client.newCall(retryRequest).execute()
                if (!retryResponse.isSuccessful) {
                    Log.e(TAG, "Retry failed: ${retryResponse.code}")
                    return@withContext null
                }
                return@withContext retryResponse.body?.string()
            }

            if (!response.isSuccessful) {
                Log.e(TAG, "$endpoint failed: ${response.code} — ${response.body?.string()}")
                return@withContext null
            }

            response.body?.string()
        } catch (e: Exception) {
            Log.e(TAG, "Error calling $endpoint: ${e.message}", e)
            null
        }
    }

    // --- Search Games ---
    suspend fun searchGames(query: String): List<IGDBGame> = withContext(Dispatchers.IO) {
        val body = """
        search "$query";
        fields id,name,cover.url,first_release_date,rating,summary,platforms.name,
               genres.name,game_modes.name,
               involved_companies.company.name,involved_companies.developer,
               age_ratings.rating,age_ratings.category,
               screenshots.url,videos.video_id;
        limit 20;
    """.trimIndent()

        val responseBody = igdbRequest("games", body) ?: return@withContext emptyList()
        parseGamesResponse(responseBody)
    }

    // --- Fetch Game Cover by Name (for Gaming Personality slideshow) ---
    suspend fun fetchGameCoverByName(gameName: String): String? = withContext(Dispatchers.IO) {
        try {
            val body = """
            search "$gameName";
            fields cover.url;
            where version_parent = null & cover != null;
            limit 1;
        """.trimIndent()
            val responseBody = igdbRequest("games", body) ?: return@withContext null
            val arr = JSONArray(responseBody)
            if (arr.length() == 0) return@withContext null
            arr.getJSONObject(0)
                .optJSONObject("cover")
                ?.optString("url")
                ?.let { "https:" + it.replace("t_thumb", "t_cover_big") }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e   // user left the screen — not an error
        } catch (e: Exception) {
            Log.e(TAG, "fetchGameCoverByName failed for $gameName: ${e.message}")
            null
        }
    }

    // --- Search Soundtracks ---
    suspend fun searchSoundtracks(query: String): List<IGDBSoundtrack> = withContext(Dispatchers.IO) {
        val body = """
            search "$query";
            fields id, name, cover.url, rating;
            where version_parent = null & cover != null;
            limit 15;
        """.trimIndent()

        val responseBody = igdbRequest("games", body) ?: return@withContext emptyList()
        parseSoundtracksResponse(responseBody)
    }

    // --- Parse games JSON ---
    private fun parseGamesResponse(responseBody: String): List<IGDBGame> {
        return try {
            val jsonArray = JSONArray(responseBody)
            val games = mutableListOf<IGDBGame>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optInt("id", 0)
                val name = obj.optString("name", "").trim()
                if (id <= 0 || name.isBlank()) continue

                val coverUrl = obj.optJSONObject("cover")
                    ?.optString("url", null)
                    ?.let { "https:" + it.replace("t_thumb", "t_cover_big") }

                val releaseYear = if (obj.has("first_release_date")) {
                    val ts = obj.optLong("first_release_date", 0)
                    if (ts > 0) {
                        val cal = java.util.Calendar.getInstance()
                        cal.timeInMillis = ts * 1000
                        cal.get(java.util.Calendar.YEAR)
                    } else null
                } else null

                val rating = if (obj.has("rating"))
                    obj.optDouble("rating") else null
                val summary = obj.optString("summary", null)

                // Parse platforms array — take the name of each platform
                val platforms = mutableListOf<String>()
                val platformsArr = obj.optJSONArray("platforms")
                if (platformsArr != null) {
                    for (p in 0 until platformsArr.length()) {
                        val pObj = platformsArr.optJSONObject(p)
                        val pName = pObj?.optString("name", "")
                        if (!pName.isNullOrBlank()) platforms.add(pName)
                    }
                }

                // Parse genres
                val genres = mutableListOf<String>()
                val genresArr = obj.optJSONArray("genres")
                if (genresArr != null) {
                    for (g in 0 until genresArr.length()) {
                        val gObj = genresArr.optJSONObject(g)
                        val gName = gObj?.optString("name", "")
                        if (!gName.isNullOrBlank()) genres.add(gName)
                    }
                }

                // Parse game modes
                val gameModes = mutableListOf<String>()
                val gameModesArr = obj.optJSONArray("game_modes")
                if (gameModesArr != null) {
                    for (m in 0 until gameModesArr.length()) {
                        val mObj = gameModesArr.optJSONObject(m)
                        val mName = mObj?.optString("name", "")
                        if (!mName.isNullOrBlank()) gameModes.add(mName)
                    }
                }

                // Parse developer (first involved_company where developer=true)
                var developer: String? = null
                val compArr = obj.optJSONArray("involved_companies")
                if (compArr != null) {
                    for (c in 0 until compArr.length()) {
                        val cObj = compArr.optJSONObject(c) ?: continue
                        if (cObj.optBoolean("developer", false)) {
                            developer = cObj.optJSONObject("company")?.optString("name")
                            break
                        }
                    }
                }

                // Parse ESRB rating (age_ratings category=1)
                var esrbRating: String? = null
                val ageRatingsArr = obj.optJSONArray("age_ratings")
                if (ageRatingsArr != null) {
                    for (r in 0 until ageRatingsArr.length()) {
                        val rObj = ageRatingsArr.optJSONObject(r) ?: continue
                        if (rObj.optInt("category", 0) == 1) {
                            esrbRating = when (rObj.optInt("rating", 0)) {
                                1 -> "RP"; 2 -> "EC"; 3 -> "E"; 4 -> "E10+"; 5 -> "T"; 6 -> "M"; 7 -> "AO"
                                else -> null
                            }
                            break
                        }
                    }
                }

                // Parse screenshots (up to 4, replace t_thumb with t_screenshot_big)
                val screenshots = mutableListOf<String>()
                val screenshotsArr = obj.optJSONArray("screenshots")
                if (screenshotsArr != null) {
                    for (s in 0 until screenshotsArr.length()) {
                        if (screenshots.size >= 4) break
                        val sObj = screenshotsArr.optJSONObject(s)
                        val url = sObj?.optString("url", "")
                        if (!url.isNullOrBlank()) {
                            screenshots.add("https:" + url.replace("t_thumb", "t_screenshot_big"))
                        }
                    }
                }

                // Parse IGDB video IDs (YouTube)
                val videoIds = mutableListOf<String>()
                val videosArr = obj.optJSONArray("videos")
                if (videosArr != null) {
                    for (v in 0 until videosArr.length()) {
                        val vId = videosArr.optJSONObject(v)?.optString("video_id", "")
                        if (!vId.isNullOrBlank()) videoIds.add(vId)
                    }
                }

                games.add(
                    IGDBGame(
                        id = id,
                        name = name,
                        coverUrl = coverUrl,
                        rating = rating,
                        releaseYear = releaseYear,
                        summary = summary,
                        platforms = platforms,
                        genres = genres,
                        gameModes = gameModes,
                        developer = developer,
                        esrbRating = esrbRating,
                        screenshots = screenshots,
                        videoIds = videoIds
                    )
                )
            }
            Log.d(TAG, "Parsed ${games.size} games")
            games
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing games: ${e.message}", e)
            emptyList()
        }
    }

    // ✅ Add to IGDBRepository.kt
    // ✅ Replace the entire fetchGameById function with this
    suspend fun fetchGameById(gameId: Int): RetroGameOfDay? = withContext(Dispatchers.IO) {
        try {
            val body = "fields name,cover.url,summary,first_release_date,rating; where id = $gameId;"
            val responseBody = igdbRequest("games", body) ?: return@withContext null
            val arr = JSONArray(responseBody)
            if (arr.length() == 0) return@withContext null
            val obj = arr.getJSONObject(0)
            val coverUrl = obj.optJSONObject("cover")?.optString("url")
                ?.replace("t_thumb", "t_cover_big")
                ?.let { if (it.startsWith("//")) "https:$it" else it }
            val releaseDate = obj.optLong("first_release_date", 0L)
            val year = if (releaseDate > 0) {
                java.util.Calendar.getInstance().apply {
                    timeInMillis = releaseDate * 1000
                }.get(java.util.Calendar.YEAR)
            } else null
            RetroGameOfDay(
                id = obj.optInt("id"),
                name = obj.optString("name"),
                coverUrl = coverUrl,
                summary = obj.optString("summary").takeIf { it.isNotBlank() },
                rating = obj.optDouble("rating", 0.0).takeIf { it > 0 },
                releaseYear = year
            )
        } catch (e: Exception) { null }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RELEASE CALENDAR
    // IGDB replaced games.category with games.game_type (same ids). New games only
    // carry game_type, so we query that first and fall back to the legacy field.
    //   0 main · 1 DLC · 2 expansion · 4 standalone exp · 8 remake · 9 remaster
    //   10 expanded game · 11 port · 13 pack · 14 update
    // Window: 14 days back ("out now") → 12 months ahead, major platforms only,
    // paged so the whole year fits (release_dates has one row per platform/region).
    // ═══════════════════════════════════════════════════════════════════════

    private val MAIN_TYPES = "(0,8,9,10,11)"
    private val DLC_TYPES  = "(1,2,4,13,14)"
    // PC, PS4, Xbox One, Switch, PS5, Xbox Series, Switch 2
    private const val MAJOR_PLATFORMS = "(6,48,49,130,167,169,508)"

    private data class CacheEntry(val at: Long, val data: List<IGDBUpcomingGame>)
    private val releaseCache = java.util.concurrent.ConcurrentHashMap<String, CacheEntry>()
    private const val RELEASE_CACHE_MS = 10 * 60 * 1000L

    /** Games (main, remakes, remasters, ports) from 14 days ago to 12 months ahead. */
    suspend fun fetchUpcomingGames(): List<IGDBUpcomingGame> =
        cachedReleases("main") { fetchReleaseWindow(MAIN_TYPES, 0, daysBack = 14, daysAhead = 365) }

    /** DLC, expansions, packs and big updates in the same window. */
    suspend fun fetchUpcomingDLC(): List<IGDBUpcomingGame> =
        cachedReleases("dlc") { fetchReleaseWindow(DLC_TYPES, 1, daysBack = 14, daysAhead = 365) }

    /** Everything releasing between two dates (used by the weekly digest + notifications). */
    suspend fun fetchReleasesBetween(fromMillis: Long, toMillis: Long): List<IGDBUpcomingGame> = withContext(Dispatchers.IO) {
        val all = (fetchUpcomingGames() + fetchUpcomingDLC())
        all.filter { it.releaseDate * 1000L in fromMillis..toMillis }
            .distinctBy { it.id }
            .sortedWith(compareBy<IGDBUpcomingGame> { it.releaseDate }.thenByDescending { it.hypes })
    }

    fun clearReleaseCache() = releaseCache.clear()

    private suspend fun cachedReleases(key: String, load: suspend () -> List<IGDBUpcomingGame>): List<IGDBUpcomingGame> {
        releaseCache[key]?.let { if (System.currentTimeMillis() - it.at < RELEASE_CACHE_MS && it.data.isNotEmpty()) return it.data }
        val fresh = load()
        if (fresh.isNotEmpty()) releaseCache[key] = CacheEntry(System.currentTimeMillis(), fresh)
        return fresh
    }

    private suspend fun fetchReleaseWindow(
        typeIds: String, defaultCategory: Int, daysBack: Int, daysAhead: Int
    ): List<IGDBUpcomingGame> = withContext(Dispatchers.IO) {
        val nowSec  = System.currentTimeMillis() / 1000
        val fromSec = nowSec - daysBack * 86400L
        val toSec   = nowSec + daysAhead * 86400L
        val fields = """
            fields date,platform.name,
                   game.id,game.name,game.cover.url,game.hypes,game.summary,game.game_type,game.category,
                   game.genres.name,game.platforms.name,game.game_modes.name,game.videos.video_id,
                   game.age_ratings.rating,game.age_ratings.category;
        """.trimIndent()

        for (typeField in listOf("game.game_type", "game.category")) {
            val merged = linkedMapOf<Int, IGDBUpcomingGame>()
            var ok = false
            for (page in 0 until 4) {
                val body = """
                    $fields
                    where date >= $fromSec & date < $toSec & $typeField = $typeIds & platform = $MAJOR_PLATFORMS;
                    sort date asc;
                    limit 500;
                    offset ${page * 500};
                """.trimIndent()
                val resp = igdbRequest("release_dates", body)
                if (resp.isNullOrBlank() || !resp.trimStart().startsWith("[")) break
                ok = true
                val parsed = parseReleaseDates(resp, defaultCategory)
                parsed.forEach { g -> if (!merged.containsKey(g.id)) merged[g.id] = g }
                if (org.json.JSONArray(resp).length() < 500) break
            }
            Log.d(TAG, "Release window ($typeField $typeIds): ${merged.size} titles")
            if (ok && merged.isNotEmpty()) return@withContext merged.values.sortedBy { it.releaseDate }
        }

        // Last resort: games table by first_release_date
        val gBody = """
            fields name,cover.url,first_release_date,summary,platforms.name,hypes,game_type,category,
                   genres.name,game_modes.name,videos.video_id,age_ratings.rating,age_ratings.category;
            where first_release_date >= $fromSec & first_release_date < $toSec & game_type = $typeIds;
            sort first_release_date asc;
            limit 500;
        """.trimIndent()
        val gResponse = igdbRequest("games", gBody) ?: return@withContext emptyList()
        parseUpcomingGames(gResponse, defaultCategory)
    }

    // --- Parse release_dates endpoint response → deduplicated IGDBUpcomingGame list ---
    private fun parseReleaseDates(responseBody: String, defaultCategory: Int): List<IGDBUpcomingGame> {
        return try {
            val jsonArray = JSONArray(responseBody)
            // Dedup by game.id — keep earliest date per game
            val byGameId = linkedMapOf<Int, IGDBUpcomingGame>()
            for (i in 0 until jsonArray.length()) {
                val entry = jsonArray.getJSONObject(i)
                val gameObj = entry.optJSONObject("game") ?: continue
                val gameId  = gameObj.optInt("id", 0)
                val name    = gameObj.optString("name", "").trim()
                if (gameId <= 0 || name.isBlank()) continue

                if (byGameId.containsKey(gameId)) continue   // already have earliest

                val dateEpochSec = entry.optLong("date", 0L)
                val coverUrl = gameObj.optJSONObject("cover")?.optString("url")
                    ?.let { "https:" + it.replace("t_thumb", "t_cover_big") }
                val hypes   = gameObj.optInt("hypes", 0)
                val summary = gameObj.optString("summary", null)?.takeIf { it.isNotBlank() }

                // Platforms — prefer game.platforms list, supplement with this entry's platform
                val platforms = mutableListOf<String>()
                gameObj.optJSONArray("platforms")?.let { pArr ->
                    for (p in 0 until pArr.length()) {
                        pArr.optJSONObject(p)?.optString("name", "")
                            ?.takeIf { it.isNotBlank() }?.let { platforms.add(it) }
                    }
                }
                if (platforms.isEmpty()) {
                    entry.optJSONObject("platform")?.optString("name", "")
                        ?.takeIf { it.isNotBlank() }?.let { platforms.add(it) }
                }

                // Genres
                val genres = mutableListOf<String>()
                gameObj.optJSONArray("genres")?.let { gArr ->
                    for (g in 0 until gArr.length())
                        gArr.optJSONObject(g)?.optString("name", "")
                            ?.takeIf { it.isNotBlank() }?.let { genres.add(it) }
                }

                // Game modes
                val gameModes = mutableListOf<String>()
                gameObj.optJSONArray("game_modes")?.let { mArr ->
                    for (m in 0 until mArr.length())
                        mArr.optJSONObject(m)?.optString("name", "")
                            ?.takeIf { it.isNotBlank() }?.let { gameModes.add(it) }
                }

                // ESRB
                var esrbRating: String? = null
                gameObj.optJSONArray("age_ratings")?.let { rArr ->
                    for (r in 0 until rArr.length()) {
                        val rObj = rArr.optJSONObject(r) ?: continue
                        if (rObj.optInt("category", 0) == 1) {
                            esrbRating = when (rObj.optInt("rating", 0)) {
                                1 -> "RP"; 2 -> "EC"; 3 -> "E"; 4 -> "E10+"; 5 -> "T"; 6 -> "M"; 7 -> "AO"
                                else -> null
                            }
                            break
                        }
                    }
                }

                // Trailer
                val trailerVideoId = gameObj.optJSONArray("videos")
                    ?.optJSONObject(0)?.optString("video_id")?.takeIf { it.isNotBlank() }

                byGameId[gameId] = IGDBUpcomingGame(
                    id = gameId, name = name, coverUrl = coverUrl,
                    releaseDate = dateEpochSec, summary = summary,
                    platforms = platforms, hypes = hypes,
                    category = gameObj.optInt("game_type", gameObj.optInt("category", defaultCategory)),
                    genres = genres, gameModes = gameModes, esrbRating = esrbRating,
                    trailerVideoId = trailerVideoId
                )
            }
            byGameId.values.toList()
        } catch (e: Exception) {
            Log.e(TAG, "parseReleaseDates: ${e.message}")
            emptyList()
        }
    }

    private fun parseUpcomingGames(responseBody: String, defaultCategory: Int): List<IGDBUpcomingGame> {
        return try {
            val jsonArray = JSONArray(responseBody)
            val list = mutableListOf<IGDBUpcomingGame>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optInt("id", 0)
                val name = obj.optString("name", "").trim()
                if (id <= 0 || name.isBlank()) continue

                val coverUrl = obj.optJSONObject("cover")?.optString("url")
                    ?.let { "https:" + it.replace("t_thumb", "t_cover_big") }
                val releaseDate = obj.optLong("first_release_date", 0L)
                val summary = obj.optString("summary", null)?.takeIf { it.isNotBlank() }
                val hypes = obj.optInt("hypes", 0)
                val category = obj.optInt("game_type", obj.optInt("category", defaultCategory))

                // Platforms
                val platforms = mutableListOf<String>()
                val pArr = obj.optJSONArray("platforms")
                if (pArr != null) {
                    for (p in 0 until pArr.length()) {
                        val pName = pArr.optJSONObject(p)?.optString("name", "")
                        if (!pName.isNullOrBlank()) platforms.add(pName)
                    }
                }

                // Genres
                val genres = mutableListOf<String>()
                val gArr = obj.optJSONArray("genres")
                if (gArr != null) {
                    for (g in 0 until gArr.length()) {
                        val gName = gArr.optJSONObject(g)?.optString("name", "")
                        if (!gName.isNullOrBlank()) genres.add(gName)
                    }
                }

                // Developer (first involved_company where developer=true)
                var developer: String? = null
                val cArr = obj.optJSONArray("involved_companies")
                if (cArr != null) {
                    for (c in 0 until cArr.length()) {
                        val cObj = cArr.optJSONObject(c) ?: continue
                        if (cObj.optBoolean("developer", false)) {
                            developer = cObj.optJSONObject("company")?.optString("name")
                            break
                        }
                    }
                }

                // ESRB rating (age_ratings.category = 1 → ESRB)
                var esrbRating: String? = null
                val rArr = obj.optJSONArray("age_ratings")
                if (rArr != null) {
                    for (r in 0 until rArr.length()) {
                        val rObj = rArr.optJSONObject(r) ?: continue
                        if (rObj.optInt("category", 0) == 1) { // 1 = ESRB
                            esrbRating = when (rObj.optInt("rating", 0)) {
                                1 -> "RP"; 2 -> "EC"; 3 -> "E"; 4 -> "E10+"; 5 -> "T"; 6 -> "M"; 7 -> "AO"
                                else -> null
                            }
                            break
                        }
                    }
                }

                // Game modes
                val gameModes = mutableListOf<String>()
                val mArr = obj.optJSONArray("game_modes")
                if (mArr != null) {
                    for (m in 0 until mArr.length()) {
                        val mName = mArr.optJSONObject(m)?.optString("name", "")
                        if (!mName.isNullOrBlank()) gameModes.add(mName)
                    }
                }

                // YouTube trailer (first video entry)
                val trailerVideoId = obj.optJSONArray("videos")
                    ?.optJSONObject(0)?.optString("video_id")?.takeIf { it.isNotBlank() }

                list.add(IGDBUpcomingGame(
                    id = id, name = name, coverUrl = coverUrl, releaseDate = releaseDate,
                    summary = summary, platforms = platforms, hypes = hypes, category = category,
                    genres = genres, developer = developer, esrbRating = esrbRating,
                    gameModes = gameModes, trailerVideoId = trailerVideoId
                ))
            }
            Log.d(TAG, "Parsed ${list.size} upcoming entries (category=$defaultCategory)")
            list
        } catch (e: Exception) {
            Log.e(TAG, "parseUpcomingGames: ${e.message}")
            emptyList()
        }
    }

    // --- Parse soundtracks JSON ---
    private fun parseSoundtracksResponse(responseBody: String): List<IGDBSoundtrack> {
        return try {
            val jsonArray = JSONArray(responseBody)
            val soundtracks = mutableListOf<IGDBSoundtrack>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optInt("id", 0)
                val name = obj.optString("name", "").trim()
                if (id <= 0 || name.isBlank()) continue

                val coverUrl = obj.optJSONObject("cover")
                    ?.optString("url", null)
                    ?.let { "https:" + it.replace("t_thumb", "t_cover_big") }

                soundtracks.add(
                    IGDBSoundtrack(
                        id = id,
                        name = "$name OST",
                        coverUrl = coverUrl,
                        gameId = id,
                        gameName = name
                    )
                )
            }
            Log.d(TAG, "Parsed ${soundtracks.size} soundtracks")
            soundtracks
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing soundtracks: ${e.message}", e)
            emptyList()
        }
    }
}
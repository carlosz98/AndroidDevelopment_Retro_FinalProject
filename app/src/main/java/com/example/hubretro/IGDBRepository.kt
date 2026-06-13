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
    val summary: String? = null
)

data class IGDBSoundtrack(
    val id: Int,
    val name: String,
    val coverUrl: String?,
    val gameId: Int? = null,
    val gameName: String? = null
)

object IGDBRepository {

    private const val CLIENT_ID = "3u77qqm0pknkp8ceuya50ff8kvmna8"
    private const val CLIENT_SECRET = "p8zzy5hckfmvchpdz2eq174c7kv7y5" // ✅ updated
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
        fields id, name, cover.url, first_release_date, rating, summary;
        where version_parent = null;
        limit 15;
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

                games.add(
                    IGDBGame(
                        id = id,
                        name = name,
                        coverUrl = coverUrl,
                        rating = rating,
                        releaseYear = releaseYear,
                        summary = summary
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
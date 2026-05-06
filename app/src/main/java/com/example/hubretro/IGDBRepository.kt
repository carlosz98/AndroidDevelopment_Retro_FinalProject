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
    private const val CLIENT_SECRET = "y482uibybwh5jtjyfn7nhmlkdnvzoo"
    private const val BASE_URL = "https://api.igdb.com/v4"
    private const val TOKEN_URL = "https://id.twitch.tv/oauth2/token"
    private const val TAG = "IGDBRepository"

    // Token state — refreshed automatically
    private var cachedToken: String? = null
    private var tokenExpiresAt: Long = 0L

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // --- Get a valid token, refreshing if needed ---
    private suspend fun getValidToken(): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedToken == null || now >= tokenExpiresAt - 3600_000L) {
            Log.d(TAG, "Token expired or missing — refreshing...")
            refreshToken()
        }
        cachedToken ?: throw Exception("Failed to obtain IGDB access token")
    }

    // --- Refresh the access token from Twitch OAuth ---
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
            if (!response.isSuccessful) {
                Log.e(TAG, "Token refresh failed: ${response.code} — ${response.body?.string()}")
                return@withContext
            }

            val responseBody = response.body?.string() ?: return@withContext
            Log.d(TAG, "Token refresh response: $responseBody")

            val json = JSONObject(responseBody)
            val newToken = json.getString("access_token")
            val expiresIn = json.getLong("expires_in")

            cachedToken = newToken
            tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000L)

            Log.d(TAG, "✅ Token refreshed! Expires in ${expiresIn / 3600} hours")
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing token: ${e.message}", e)
        }
    }

    // --- Search Games ---
    suspend fun searchGames(query: String): List<IGDBGame> = withContext(Dispatchers.IO) {
        try {
            val token = getValidToken()

            val body = """
                search "$query";
                fields id, name, cover.url, first_release_date, rating, summary;
                where version_parent = null;
                limit 10;
            """.trimIndent()

            val request = Request.Builder()
                .url("$BASE_URL/games")
                .post(body.toRequestBody("text/plain".toMediaType()))
                .header("Client-ID", CLIENT_ID)
                .header("Authorization", "Bearer $token")
                .build()

            val response = client.newCall(request).execute()

            // 401 — force refresh and retry once
            if (response.code == 401) {
                Log.w(TAG, "401 on games search — forcing token refresh")
                cachedToken = null
                val freshToken = getValidToken()
                val retryRequest = Request.Builder()
                    .url("$BASE_URL/games")
                    .post(body.toRequestBody("text/plain".toMediaType()))
                    .header("Client-ID", CLIENT_ID)
                    .header("Authorization", "Bearer $freshToken")
                    .build()
                return@withContext parseGamesResponse(
                    client.newCall(retryRequest).execute().body?.string()
                )
            }

            if (!response.isSuccessful) {
                Log.e(TAG, "Games search failed: ${response.code}")
                return@withContext emptyList()
            }

            parseGamesResponse(response.body?.string())
        } catch (e: Exception) {
            Log.e(TAG, "Error searching games: ${e.message}", e)
            emptyList()
        }
    }

    // --- Search Soundtracks ---
    suspend fun searchSoundtracks(query: String): List<IGDBSoundtrack> = withContext(Dispatchers.IO) {
        try {
            val token = getValidToken()

            val body = """
                search "$query";
                fields id, name, cover.url, rating;
                where version_parent = null & cover != null;
                limit 10;
            """.trimIndent()

            val request = Request.Builder()
                .url("$BASE_URL/games")
                .post(body.toRequestBody("text/plain".toMediaType()))
                .header("Client-ID", CLIENT_ID)
                .header("Authorization", "Bearer $token")
                .build()

            val response = client.newCall(request).execute()

            // 401 — force refresh and retry once
            if (response.code == 401) {
                Log.w(TAG, "401 on soundtrack search — forcing token refresh")
                cachedToken = null
                val freshToken = getValidToken()
                val retryRequest = Request.Builder()
                    .url("$BASE_URL/games")
                    .post(body.toRequestBody("text/plain".toMediaType()))
                    .header("Client-ID", CLIENT_ID)
                    .header("Authorization", "Bearer $freshToken")
                    .build()
                return@withContext parseSoundtracksResponse(
                    client.newCall(retryRequest).execute().body?.string()
                )
            }

            if (!response.isSuccessful) {
                Log.e(TAG, "Soundtrack search failed: ${response.code}")
                return@withContext emptyList()
            }

            parseSoundtracksResponse(response.body?.string())
        } catch (e: Exception) {
            Log.e(TAG, "Error searching soundtracks: ${e.message}", e)
            emptyList()
        }
    }

    // --- Parse games JSON response ---
    private fun parseGamesResponse(responseBody: String?): List<IGDBGame> {
        if (responseBody == null) return emptyList()
        return try {
            val jsonArray = JSONArray(responseBody)
            val games = mutableListOf<IGDBGame>()
            for (i in 0 until jsonArray.length()) {
                val gameObj = jsonArray.getJSONObject(i)
                val id = gameObj.optInt("id", 0)
                val name = gameObj.optString("name", "Unknown Game")

                var coverUrl: String? = null
                if (gameObj.has("cover")) {
                    val coverObj = gameObj.optJSONObject("cover")
                    val rawUrl = coverObj?.optString("url", null)
                    coverUrl = rawUrl?.let {
                        "https:" + it.replace("t_thumb", "t_cover_big")
                    }
                }

                var releaseYear: Int? = null
                if (gameObj.has("first_release_date")) {
                    val timestamp = gameObj.optLong("first_release_date", 0)
                    if (timestamp > 0) {
                        val cal = java.util.Calendar.getInstance()
                        cal.timeInMillis = timestamp * 1000
                        releaseYear = cal.get(java.util.Calendar.YEAR)
                    }
                }

                val rating = if (gameObj.has("rating"))
                    gameObj.optDouble("rating", 0.0) else null
                val summary = gameObj.optString("summary", null)

                if (id > 0 && name.isNotBlank()) {
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
            }
            games
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing games: ${e.message}", e)
            emptyList()
        }
    }

    // --- Parse soundtracks JSON response ---
    private fun parseSoundtracksResponse(responseBody: String?): List<IGDBSoundtrack> {
        if (responseBody == null) return emptyList()
        return try {
            val jsonArray = JSONArray(responseBody)
            val soundtracks = mutableListOf<IGDBSoundtrack>()
            for (i in 0 until jsonArray.length()) {
                val gameObj = jsonArray.getJSONObject(i)
                val id = gameObj.optInt("id", 0)
                val name = gameObj.optString("name", "Unknown")

                var coverUrl: String? = null
                if (gameObj.has("cover")) {
                    val coverObj = gameObj.optJSONObject("cover")
                    val rawUrl = coverObj?.optString("url", null)
                    coverUrl = rawUrl?.let {
                        "https:" + it.replace("t_thumb", "t_cover_big")
                    }
                }

                if (id > 0 && name.isNotBlank()) {
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
            }
            soundtracks
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing soundtracks: ${e.message}", e)
            emptyList()
        }
    }
}
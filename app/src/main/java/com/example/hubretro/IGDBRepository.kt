package com.example.hubretro

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
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
    private const val ACCESS_TOKEN = "itso0ce9u6k5em9k4h0fwsaf13djxv"
    private const val BASE_URL = "https://api.igdb.com/v4"
    private const val TAG = "IGDBRepository"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // --- Search Games ---
    suspend fun searchGames(query: String): List<IGDBGame> = withContext(Dispatchers.IO) {
        try {
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
                .header("Authorization", "Bearer $ACCESS_TOKEN")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Games search failed: ${response.code}")
                return@withContext emptyList()
            }

            val responseBody = response.body?.string() ?: return@withContext emptyList()
            Log.d(TAG, "Games response: $responseBody")

            val jsonArray = JSONArray(responseBody)
            val games = mutableListOf<IGDBGame>()

            for (i in 0 until jsonArray.length()) {
                val gameObj = jsonArray.getJSONObject(i)
                val id = gameObj.optInt("id", 0)
                val name = gameObj.optString("name", "Unknown Game")

                // Get cover URL
                var coverUrl: String? = null
                if (gameObj.has("cover")) {
                    val coverObj = gameObj.optJSONObject("cover")
                    val rawUrl = coverObj?.optString("url", null)
                    coverUrl = rawUrl?.let {
                        // Replace thumbnail size with large size
                        "https:" + it.replace("t_thumb", "t_cover_big")
                    }
                }

                // Get release year
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
                    gameObj.optDouble("rating", 0.0)
                else null

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
            Log.e(TAG, "Error searching games: ${e.message}", e)
            emptyList()
        }
    }

    // --- Search Game Soundtracks ---
    suspend fun searchSoundtracks(query: String): List<IGDBSoundtrack> = withContext(Dispatchers.IO) {
        try {
            // Search for games matching query — soundtracks are linked to games
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
                .header("Authorization", "Bearer $ACCESS_TOKEN")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Soundtrack search failed: ${response.code}")
                return@withContext emptyList()
            }

            val responseBody = response.body?.string() ?: return@withContext emptyList()
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
                    // Use bigger image for vinyl display
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
            Log.e(TAG, "Error searching soundtracks: ${e.message}", e)
            emptyList()
        }
    }
}
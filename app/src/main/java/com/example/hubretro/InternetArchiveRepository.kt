package com.example.hubretro

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class ArchiveItem(
    val id: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String?,
    val webUrl: String,
    val category: ArchiveCategory,
    val year: String? = null,
    val creator: String? = null
)

enum class ArchiveCategory {
    ALBUM, MAGAZINE, ARTICLE
}

object InternetArchiveRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val BASE_URL = "https://archive.org"
    private const val TAG = "ArchiveRepo"

    // --- Core search function ---
    private suspend fun searchArchive(
        query: String,
        mediaType: String,
        rows: Int = 20,
        category: ArchiveCategory
    ): List<ArchiveItem> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "$BASE_URL/advancedsearch.php" +
                    "?q=$encodedQuery" +
                    "+AND+mediatype%3A($mediaType)" +
                    "&fl[]=identifier" +
                    "&fl[]=title" +
                    "&fl[]=description" +
                    "&fl[]=year" +
                    "&fl[]=creator" +
                    "&fl[]=subject" +
                    "&rows=$rows" +
                    "&page=1" +
                    "&output=json"

            Log.d(TAG, "Fetching: $url")

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "RetroHubApp/1.0")
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "HTTP error: ${response.code}")
                return@withContext emptyList()
            }

            val body = response.body?.string()
            if (body.isNullOrBlank()) {
                Log.e(TAG, "Empty response body")
                return@withContext emptyList()
            }

            Log.d(TAG, "Response received, parsing...")

            val json = JSONObject(body)
            val responseObj = json.optJSONObject("response")
                ?: return@withContext emptyList()
            val docs = responseObj.optJSONArray("docs")
                ?: return@withContext emptyList()

            Log.d(TAG, "Found ${docs.length()} results")

            val results = mutableListOf<ArchiveItem>()

            for (i in 0 until docs.length()) {
                try {
                    val doc = docs.getJSONObject(i)
                    val id = doc.optString("identifier", "").trim()
                    if (id.isBlank()) continue

                    val title = doc.optString("title", "Unknown Title").trim()
                    if (title.isBlank() || title == "Unknown Title") continue

                    val rawDesc = doc.optString("description", "")
                    val description = rawDesc.trim().take(200).let {
                        if (rawDesc.length > 200) "$it..." else it
                    }

                    val year = doc.optString("year", "").ifBlank { null }
                    val creator = doc.optString("creator", "").ifBlank { null }

                    results.add(
                        ArchiveItem(
                            id = id,
                            title = title,
                            description = description,
                            thumbnailUrl = "$BASE_URL/services/img/$id",
                            webUrl = "$BASE_URL/details/$id",
                            category = category,
                            year = year,
                            creator = creator
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing item $i: ${e.message}")
                    continue
                }
            }

            Log.d(TAG, "Parsed ${results.size} valid items for category $category")
            results

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching archive: ${e.message}", e)
            emptyList()
        }
    }

    // --- Game Soundtracks ---
    suspend fun fetchGameSoundtracks(query: String = ""): List<ArchiveItem> {
        val searchQuery = if (query.isBlank())
            "subject:(video game soundtrack) OR subject:(game music) OR subject:(chiptune) OR subject:(game ost)"
        else
            "$query game soundtrack music"
        return searchArchive(
            query = searchQuery,
            mediaType = "audio",
            rows = 20,
            category = ArchiveCategory.ALBUM
        )
    }

    // --- Retro Magazines ---
    suspend fun fetchRetroMagazines(query: String = ""): List<ArchiveItem> {
        val searchQuery = if (query.isBlank())
            "title:(GamePro) OR title:(Nintendo Power) OR title:(Electronic Gaming Monthly) OR title:(Game Informer) OR title:(Retro Gamer) OR subject:(video game magazine)"
        else
            "$query gaming magazine retro"
        return searchArchive(
            query = searchQuery,
            mediaType = "texts",
            rows = 20,
            category = ArchiveCategory.MAGAZINE
        )
    }

    // --- Retro Articles ---
    suspend fun fetchRetroArticles(query: String = ""): List<ArchiveItem> {
        val searchQuery = if (query.isBlank())
            "subject:(retro gaming) OR subject:(classic video games) OR subject:(video game history) OR subject:(arcade games)"
        else
            "$query retro gaming history"
        return searchArchive(
            query = searchQuery,
            mediaType = "texts",
            rows = 20,
            category = ArchiveCategory.ARTICLE
        )
    }

    // --- Universal search ---
    suspend fun searchAll(query: String): List<ArchiveItem> {
        if (query.isBlank()) return emptyList()
        val albums = fetchGameSoundtracks(query)
        val magazines = fetchRetroMagazines(query)
        val articles = fetchRetroArticles(query)
        return albums + magazines + articles
    }
}
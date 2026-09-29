package com.example.hubretro

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class EbayListing(
    val itemId: String        = "",
    val title: String         = "",
    val price: String         = "",
    val currency: String      = "USD",
    val imageUrl: String      = "",
    val additionalImages: List<String> = emptyList(),
    val condition: String     = "",
    val seller: String        = "",
    val sellerFeedback: Int   = 0,
    val itemUrl: String       = "",
    val location: String      = "",
    val shippingCost: String  = "",
    val listingType: String   = "",
    val endDate: String       = "",
    val shortDescription: String = ""
)

object EbayRepository {

    // Video game category IDs on eBay
    // 139973 = Video Games, 1249 = Video Game Consoles, 139971 = Accessories
    private const val GAME_CATEGORIES = "categoryIds:{139973|1249|139971|64482}"

    suspend fun searchListings(query: String, limit: Int = 20, sortBy: String = "newlyListed"): List<EbayListing> = withContext(Dispatchers.IO) {
        try {
            val token = EbayAuthRepository.getToken()
            if (token.isBlank()) {
                android.util.Log.e("EbaySearch", "Token is blank, aborting search")
                return@withContext emptyList()
            }

            val encodedQuery = Uri.encode(query)
            val url = "https://api.ebay.com/buy/browse/v1/item_summary/search" +
                    "?q=$encodedQuery" +
                    "&limit=$limit" +
                    "&sort=$sortBy" +
                    "&filter=$GAME_CATEGORIES"

            val client = OkHttpClient()
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("X-EBAY-C-MARKETPLACE-ID", "EBAY_US")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()
            android.util.Log.d("EbaySearch", "Response code: ${response.code}")
            android.util.Log.d("EbaySearch", "Response body: ${body.take(800)}")
            val json = JSONObject(body)
            val items = json.optJSONArray("itemSummaries") ?: return@withContext emptyList()

            (0 until items.length()).mapNotNull { i ->
                try {
                    val item       = items.getJSONObject(i)
                    val priceObj   = item.optJSONObject("price")
                    val imageObj   = item.optJSONObject("image")
                    val sellerObj  = item.optJSONObject("seller")
                    val shippingArr = item.optJSONArray("shippingOptions")
                    val shippingCostObj = shippingArr?.optJSONObject(0)?.optJSONObject("shippingCost")

                    // Additional images
                    val additionalImagesArr = item.optJSONArray("additionalImages")
                    val extraImages = if (additionalImagesArr != null) {
                        (0 until additionalImagesArr.length()).map { j ->
                            additionalImagesArr.getJSONObject(j).optString("imageUrl", "")
                        }.filter { it.isNotBlank() }
                    } else emptyList()

                    EbayListing(
                        itemId        = item.optString("itemId"),
                        title         = item.optString("title"),
                        price         = priceObj?.optString("value") ?: "?",
                        currency      = priceObj?.optString("currency") ?: "USD",
                        imageUrl      = imageObj?.optString("imageUrl") ?: "",
                        additionalImages = extraImages,
                        condition     = item.optString("condition", "Used"),
                        seller        = sellerObj?.optString("username") ?: "",
                        sellerFeedback = sellerObj?.optInt("feedbackScore", 0) ?: 0,
                        itemUrl       = item.optString("itemWebUrl"),
                        location      = item.optString("itemLocation", ""),
                        shippingCost  = shippingCostObj?.optString("value") ?: "?",
                        listingType   = item.optString("buyingOptions", ""),
                        endDate       = item.optString("itemEndDate", ""),
                        shortDescription = item.optString("shortDescription", "")
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) {
            android.util.Log.e("EbaySearch", "Search error: ${e.message}", e)
            emptyList()
        }
    }
}
package com.example.hubretro

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject

object EbayAuthRepository {

    // ⚠️ Do NOT hardcode keys here. Set EBAY_CLIENT_ID and EBAY_CLIENT_SECRET
    // in your local.properties file and read them via BuildConfig.
    private const val CLIENT_ID     = BuildConfig.EBAY_CLIENT_ID
    private const val CLIENT_SECRET = BuildConfig.EBAY_CLIENT_SECRET

    private var cachedToken: String? = null
    private var tokenExpiry: Long    = 0L

    suspend fun getToken(): String {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiry) {
            return cachedToken!!
        }
        return withContext(Dispatchers.IO) {
            try {
                val credentials = "$CLIENT_ID:$CLIENT_SECRET"
                val encoded = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
                val client = OkHttpClient()
                val body = "grant_type=client_credentials&scope=https%3A%2F%2Fapi.ebay.com%2Foauth%2Fapi_scope"
                    .toRequestBody("application/x-www-form-urlencoded".toMediaType())
                val request = Request.Builder()
                    .url("https://api.ebay.com/identity/v1/oauth2/token")
                    .post(body)
                    .addHeader("Authorization", "Basic $encoded")
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build()
                val response = client.newCall(request).execute()
                val bodyStr = response.body?.string() ?: ""
                android.util.Log.d("EbayAuth", "Response code: ${response.code}")
                android.util.Log.d("EbayAuth", "Response body: $bodyStr")
                val json = JSONObject(bodyStr)
                cachedToken = json.getString("access_token")
                tokenExpiry = System.currentTimeMillis() + (json.getLong("expires_in") * 1000L) - 60000L
                cachedToken!!
            } catch (e: Exception) {
                android.util.Log.e("EbayAuth", "Token error: ${e.message}", e)
                ""
            }
        }
    }
}
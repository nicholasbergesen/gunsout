package com.gunsout.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed class LookupResult {
    data class Success(val item: NutritionItem) : LookupResult()
    object MissingKey : LookupResult()
    object NoResult : LookupResult()
    object Unauthorized : LookupResult()
    object RateLimited : LookupResult()
    data class NetworkError(val message: String) : LookupResult()
}

/**
 * Client for the optional CalorieNinjas ingredient lookup.
 * Bring-your-own API key from calorieninjas.com. Single GET endpoint, no background calls.
 * Caller's typed query is the only thing sent over the wire.
 */
@Singleton
class CalorieNinjasClient @Inject constructor(
    private val apiKeyStore: ApiKeyStore
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun lookup(query: String): LookupResult = withContext(Dispatchers.IO) {
        val key = apiKeyStore.getCalorieNinjasKey() ?: return@withContext LookupResult.MissingKey
        if (query.isBlank()) return@withContext LookupResult.NoResult

        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val request = Request.Builder()
            .url("https://api.calorieninjas.com/v1/nutrition?query=$encoded")
            .header("X-Api-Key", key)
            .get()
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                when (response.code) {
                    401, 403 -> LookupResult.Unauthorized
                    429 -> LookupResult.RateLimited
                    in 200..299 -> {
                        val body = response.body?.string().orEmpty()
                        val parsed = json.decodeFromString(CalorieNinjasResponse.serializer(), body)
                        parsed.items.firstOrNull()?.let { LookupResult.Success(it) } ?: LookupResult.NoResult
                    }
                    else -> LookupResult.NetworkError("HTTP ${response.code}")
                }
            }
        }.getOrElse { e ->
            when (e) {
                is SocketTimeoutException -> LookupResult.NetworkError("Network timeout. Try again.")
                is IOException -> LookupResult.NetworkError("Offline or unreachable.")
                else -> LookupResult.NetworkError(e.message ?: "Lookup failed.")
            }
        }
    }
}

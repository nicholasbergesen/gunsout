package com.gunsout.data.remote

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Lightweight tests for CalorieNinjas response parsing and result mapping using MockWebServer.
 * The real CalorieNinjasClient depends on ApiKeyStore which needs Android Context, so here we
 * test the parsing layer directly with the same Json config and the same response shape.
 */
class CalorieNinjasParseTest {

    private lateinit var server: MockWebServer
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()

    @Before fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After fun tearDown() {
        server.shutdown()
    }

    private fun lookup(query: String, apiKey: String?): LookupResult = runBlocking {
        if (apiKey.isNullOrBlank()) return@runBlocking LookupResult.MissingKey
        if (query.isBlank()) return@runBlocking LookupResult.NoResult
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(server.url("/v1/nutrition").newBuilder().addQueryParameter("query", query).build())
                .header("X-Api-Key", apiKey)
                .get().build()
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
                if (e is IOException) LookupResult.NetworkError("offline") else LookupResult.NetworkError(e.message ?: "")
            }
        }
    }

    @Test fun `missing key short-circuits`() {
        val result = lookup("oats", apiKey = null)
        assertTrue(result is LookupResult.MissingKey)
    }

    @Test fun `successful response parses first item`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """
            {
              "items": [
                {
                  "name": "oats",
                  "calories": 379.0,
                  "serving_size_g": 100.0,
                  "fat_total_g": 7.0,
                  "protein_g": 13.0,
                  "carbohydrates_total_g": 68.0
                }
              ]
            }
            """.trimIndent()
        ))
        val result = lookup("100g oats", apiKey = "k")
        assertTrue(result is LookupResult.Success)
        val item = (result as LookupResult.Success).item
        assertEquals("oats", item.name)
        assertEquals(379.0, item.calories, 0.001)
        assertEquals(13.0, item.proteinG, 0.001)
    }

    @Test fun `empty items maps to NoResult`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"items":[]}"""))
        val result = lookup("xyz", apiKey = "k")
        assertTrue(result is LookupResult.NoResult)
    }

    @Test fun `401 maps to Unauthorized`() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))
        val result = lookup("oats", apiKey = "k")
        assertTrue(result is LookupResult.Unauthorized)
    }

    @Test fun `429 maps to RateLimited`() {
        server.enqueue(MockResponse().setResponseCode(429).setBody("{}"))
        val result = lookup("oats", apiKey = "k")
        assertTrue(result is LookupResult.RateLimited)
    }

    @Test fun `unknown JSON fields are ignored`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """
            {"items":[{"name":"a","calories":1.0,"new_field":"x","another":42}]}
            """.trimIndent()
        ))
        val result = lookup("a", apiKey = "k")
        assertTrue(result is LookupResult.Success)
    }
}

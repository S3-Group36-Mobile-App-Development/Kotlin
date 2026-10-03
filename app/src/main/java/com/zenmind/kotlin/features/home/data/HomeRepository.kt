package com.zenmind.kotlin.features.home.data

import com.zenmind.kotlin.features.home.model.HomeData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

interface HomeRepository {
    suspend fun getHomeData(): HomeData
}

private const val BASE_URL = "https://zenmind-api.onrender.com/api/v1/"

private const val TIMEOUT_MS = 120_000

/**
 * - Con token: GET usuarios/me (nombre).
 * - Sin token (aún no hay login): GET health, para despertar el back y entrar como invitado.
 */
class NetworkHomeRepository(
    private val tokenProvider: () -> String? = { null }
) : HomeRepository {

    override suspend fun getHomeData(): HomeData = withContext(Dispatchers.IO) {
        val token = tokenProvider()
        if (token == null) {
            get("health", null)
            HomeData(userName = null)
        } else {
            val json = JSONObject(get("usuarios/me", token))
            HomeData(userName = json.optString("nombreVisible").ifBlank { null })
        }
    }

    private fun get(path: String, token: String?): String {
        val conn = URL(BASE_URL + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.setRequestProperty("Accept", "application/json")
            if (token != null) conn.setRequestProperty("Authorization", "Bearer $token")
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("HTTP $code en $path")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}

package com.zenmind.kotlin.core.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "https://zenmind-api-86x0.onrender.com/"
    @Volatile private var verifiedAccessToken: String? = null

    fun setVerifiedAccessToken(token: String?) {
        verifiedAccessToken = token
    }

    // Timeouts amplios para tolerar el cold start de Render
    // (el plan gratuito duerme el servicio tras inactividad y el
    // primer request puede tardar hasta ~60s en responder).
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val authorization = request.header("Authorization")
                // This endpoint is part of session restoration, not a feature request.
                val verifiesSession = request.method == "GET" && request.url.encodedPath == "/api/v1/users/me"
                if (!authorization.isNullOrBlank() && !verifiesSession &&
                    verifiedAccessToken?.let { authorization == "Bearer $it" } != true) {
                    throw IOException("La sesión debe verificarse antes de consultar el servidor.")
                }
                chain.proceed(request)
            }
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

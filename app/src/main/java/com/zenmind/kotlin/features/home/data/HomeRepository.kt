package com.zenmind.kotlin.features.home.data

import com.zenmind.kotlin.core.storage.TokenStorage
import com.zenmind.kotlin.features.home.model.HomeData

interface HomeRepository {
    suspend fun getHomeData(): HomeData
}

class NetworkHomeRepository(
    private val api: HomeApiService,
    private val tokenStorage: TokenStorage
) : HomeRepository {

    override suspend fun getHomeData(): HomeData {
        // Invitado
        val token = tokenStorage.getAccessToken() ?: return HomeData(userName = null)

        // Con sesión
        return try {
            HomeData(userName = api.getProfile("Bearer $token").nombreVisible)
        } catch (e: Exception) {
            HomeData(userName = null)
        }
    }
}

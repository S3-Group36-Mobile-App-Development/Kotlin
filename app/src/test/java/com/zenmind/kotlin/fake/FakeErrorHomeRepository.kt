package com.zenmind.kotlin.fake

import com.zenmind.kotlin.features.home.data.HomeRepository
import com.zenmind.kotlin.features.home.model.HomeData
import java.io.IOException

/** Simula que la API falla (por ejemplo, timeout). */
class FakeErrorHomeRepository : HomeRepository {
    override suspend fun getHomeData(): HomeData {
        throw IOException("timeout")
    }
}

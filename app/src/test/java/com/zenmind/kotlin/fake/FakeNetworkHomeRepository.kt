package com.zenmind.kotlin.fake

import com.zenmind.kotlin.features.home.data.HomeRepository
import com.zenmind.kotlin.features.home.model.HomeData

class FakeNetworkHomeRepository : HomeRepository {
    override suspend fun getHomeData(): HomeData {
        return FakeDataSource.homeData
    }
}

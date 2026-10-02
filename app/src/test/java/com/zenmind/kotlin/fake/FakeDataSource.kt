package com.zenmind.kotlin.fake

import com.zenmind.kotlin.features.home.model.HomeData

object FakeDataSource {
    const val userName = "Leo"
    val homeData = HomeData(userName = userName)
}

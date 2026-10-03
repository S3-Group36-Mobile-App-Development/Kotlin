package com.zenmind.kotlin.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import com.zenmind.kotlin.core.network.ApiClient
import com.zenmind.kotlin.core.storage.TokenStorage
import com.zenmind.kotlin.core.telemetry.TelemetryApiService
import com.zenmind.kotlin.core.telemetry.TelemetryRepository
import com.zenmind.kotlin.features.dailycheckin.data.CheckInApiService
import com.zenmind.kotlin.features.dailycheckin.data.DailyCheckInRepository
import com.zenmind.kotlin.features.dailycheckin.data.NetworkDailyCheckInRepository
import com.zenmind.kotlin.features.home.data.HomeApiService
import com.zenmind.kotlin.features.home.data.HomeRepository
import com.zenmind.kotlin.features.home.data.NetworkHomeRepository

interface AppContainer {
    val homeRepository: HomeRepository
    val dailyCheckInRepository: DailyCheckInRepository
    val telemetryRepository: TelemetryRepository
}

class DefaultAppContainer(
    context: Context,
    private val applicationScope: CoroutineScope
) : AppContainer {

    private val tokenStorage = TokenStorage(context)

    private val checkInApi: CheckInApiService by lazy {
        ApiClient.retrofit.create(CheckInApiService::class.java)
    }

    private val homeApi: HomeApiService by lazy {
        ApiClient.retrofit.create(HomeApiService::class.java)
    }

    override val homeRepository: HomeRepository by lazy {
        NetworkHomeRepository(homeApi, tokenStorage)
    }

    override val dailyCheckInRepository: DailyCheckInRepository by lazy {
        NetworkDailyCheckInRepository(checkInApi, tokenStorage)
    }

    override val telemetryRepository: TelemetryRepository by lazy {
        TelemetryRepository(ApiClient.retrofit.create(TelemetryApiService::class.java), applicationScope)
    }
}

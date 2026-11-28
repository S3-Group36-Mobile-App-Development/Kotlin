package com.zenmind.kotlin.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.zenmind.kotlin.core.database.AppDatabase
import com.zenmind.kotlin.core.network.ApiClient
import com.zenmind.kotlin.core.network.ConnectivityObserver
import com.zenmind.kotlin.core.storage.TokenStorage
import com.zenmind.kotlin.core.telemetry.TelemetryApiService
import com.zenmind.kotlin.core.telemetry.TelemetryRepository
import com.zenmind.kotlin.features.dailycheckin.data.CheckInApiService
import com.zenmind.kotlin.features.dailycheckin.data.DailyCheckInRepository
import com.zenmind.kotlin.features.dailycheckin.data.FlashcardsCache
import com.zenmind.kotlin.features.dailycheckin.data.OfflineFirstDailyCheckInRepository
import com.zenmind.kotlin.features.games.data.GameSyncScheduler
import com.zenmind.kotlin.features.panic.data.PanicRepository
import com.zenmind.kotlin.features.protocols.data.ProtocolApiService
import com.zenmind.kotlin.features.protocols.data.ProtocolCache
import com.zenmind.kotlin.features.protocols.data.ProtocolRepository
import kotlinx.coroutines.CancellationException

interface AppContainer {
    val dailyCheckInRepository: DailyCheckInRepository
    val telemetryRepository: TelemetryRepository
    val connectivityObserver: ConnectivityObserver
    val protocolRepository: ProtocolRepository
    val panicRepository: PanicRepository
}

class DefaultAppContainer(
    context: Context,
    private val applicationScope: CoroutineScope
) : AppContainer {

    private val tokenStorage = TokenStorage(context)

    override val connectivityObserver: ConnectivityObserver by lazy {
        ConnectivityObserver(context)
    }

    private val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    private val checkInApi: CheckInApiService by lazy {
        ApiClient.retrofit.create(CheckInApiService::class.java)
    }

    private val flashcardsCache: FlashcardsCache by lazy {
        FlashcardsCache()
    }

    override val dailyCheckInRepository: DailyCheckInRepository by lazy {
        OfflineFirstDailyCheckInRepository(
            checkInApi,
            tokenStorage,
            flashcardsCache,
            database.getCheckinDao(),
            database.getMoodDao()
        )
    }

    override val panicRepository: PanicRepository by lazy {
        PanicRepository(database.emergencyContactDao())
    }

    override val protocolRepository: ProtocolRepository by lazy {
        ProtocolRepository(
            ApiClient.retrofit.create(ProtocolApiService::class.java),
            database.protocolDao(),
            ProtocolCache(),
            tokenStorage
        )
    }

    override val telemetryRepository: TelemetryRepository by lazy {
        TelemetryRepository(ApiClient.retrofit.create(TelemetryApiService::class.java), applicationScope)
    }

    init {
        applicationScope.launch {
            connectivityObserver.onlineFlow.collect { online ->
                if (online) {
                    applicationScope.launch {
                        try { GameSyncScheduler.enqueuePending(context) }
                        catch (e: CancellationException) { throw e }
                        catch (_: Exception) { }
                    }
                    runCatching { dailyCheckInRepository.syncPending() }
                    runCatching { protocolRepository.syncPending() }
                }
            }
        }
    }
}

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
import com.zenmind.kotlin.features.dailycheckin.data.CheckInDraftStore
import com.zenmind.kotlin.features.dailycheckin.data.DailyCheckInRepository
import com.zenmind.kotlin.features.dailycheckin.data.FlashcardFileStore
import com.zenmind.kotlin.features.dailycheckin.data.FlashcardsCache
import com.zenmind.kotlin.features.dailycheckin.data.OfflineFirstDailyCheckInRepository
import com.zenmind.kotlin.features.games.data.GameSyncScheduler
import kotlinx.coroutines.CancellationException

interface AppContainer {
    val dailyCheckInRepository: DailyCheckInRepository
    val telemetryRepository: TelemetryRepository
    val connectivityObserver: ConnectivityObserver
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
            FlashcardFileStore(context.filesDir),
            CheckInDraftStore(context),
            database.getCheckinDao(),
            database.getMoodDao()
        )
    }

    override val telemetryRepository: TelemetryRepository by lazy {
        TelemetryRepository(ApiClient.retrofit.create(TelemetryApiService::class.java), applicationScope)
    }

    init {
        applicationScope.launch {
            dailyCheckInRepository.syncEvents.collect { count ->
                telemetryRepository.featureUse(
                    "checkin",
                    mapOf("accion" to "sincronizado", "cantidad" to count.toString())
                )
            }
        }

        applicationScope.launch {
            connectivityObserver.onlineFlow.collect { online ->
                if (online) {
                    applicationScope.launch {
                        try { GameSyncScheduler.enqueuePending(context) }
                        catch (e: CancellationException) { throw e }
                        catch (_: Exception) { /* The next connection or login can enqueue again. */ }
                    }
                    try {
                        dailyCheckInRepository.syncPending()
                        dailyCheckInRepository.prefetch()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }
}

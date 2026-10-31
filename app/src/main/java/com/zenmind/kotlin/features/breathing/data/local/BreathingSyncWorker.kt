package com.zenmind.kotlin.features.breathing.data.local

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zenmind.kotlin.core.database.AppDatabase
import com.zenmind.kotlin.core.network.ApiClient
import com.zenmind.kotlin.core.storage.TokenStorage
import com.zenmind.kotlin.features.breathing.data.BreathingApiService
import com.zenmind.kotlin.features.breathing.data.CrearSesionRequest

/**
 * Sincroniza con el backend las sesiones de respiracion que quedaron
 * pendientes en Room (synced = 0).
 *
 * Estrategia de conectividad eventual: WorkManager solo ejecuta este
 * worker cuando hay red. Por cada pendiente hace 
 * crearSesion + completarSesion; solo marca synced = 1
 * si ambas llamadas tuvieron exito. Si algo falla, devuelve retry()
 * y WorkManager reintenta con backoff exponencial.
 */
class BreathingSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.getInstance(applicationContext).breathingSessionDao()

        val token = TokenStorage(applicationContext).getAccessToken()
            ?: return Result.success()
        val bearer = "Bearer $token"

        val service = ApiClient.retrofit.create(BreathingApiService::class.java)

        val pending = dao.getPending()
        if (pending.isEmpty()) return Result.success()

        return try {
            for (session in pending) {
                val crear = service.crearSesion(
                    bearer,
                    CrearSesionRequest(ejercicioId = session.ejercicioId)
                )
                if (!crear.isSuccessful) return Result.retry()

                val backId = crear.body()?.id ?: return Result.retry()

                val completar = service.completarSesion(bearer, backId)
                if (!completar.isSuccessful) return Result.retry()

                dao.markSynced(session.id)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

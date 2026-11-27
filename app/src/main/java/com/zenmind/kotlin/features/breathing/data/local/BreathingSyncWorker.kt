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
        if (token.isNullOrBlank()) {
            android.util.Log.d("BreathingSyncWorker", "Token es NULO o VACIO")
            return Result.success()
        }
        val bearer = "Bearer $token"

        val service = ApiClient.retrofit.create(BreathingApiService::class.java)

        // Limpiar sesiones corruptas con ejercicioId <= 0 de pruebas pasadas
        dao.deleteInvalidSessions()

        val pending = dao.getPending()
        android.util.Log.d("BreathingSyncWorker", "Pendientes en Room: ${pending.size}")
        if (pending.isEmpty()) return Result.success()

        return try {
            for (session in pending) {
                val exerciseIdStr = session.ejercicioId.toString()
                android.util.Log.d("BreathingSyncWorker", "Enviando sesion local id=${session.id}, ejercicioId=$exerciseIdStr")
                val crear = service.crearSesion(
                    bearer,
                    CrearSesionRequest(ejercicioId = exerciseIdStr)
                )
                android.util.Log.d("BreathingSyncWorker", "crearSesion HTTP code=${crear.code()}, isSuccessful=${crear.isSuccessful}")
                if (!crear.isSuccessful) {
                    android.util.Log.e("BreathingSyncWorker", "crearSesion errorBody=${crear.errorBody()?.string()}")
                    return Result.retry()
                }

                val backId = crear.body()?.id ?: return Result.retry()
                android.util.Log.d("BreathingSyncWorker", "backId recibido=$backId")

                val completar = service.completarSesion(bearer, backId)
                android.util.Log.d("BreathingSyncWorker", "completarSesion HTTP code=${completar.code()}, isSuccessful=${completar.isSuccessful}")
                if (!completar.isSuccessful) {
                    android.util.Log.e("BreathingSyncWorker", "completarSesion errorBody=${completar.errorBody()?.string()}")
                    return Result.retry()
                }

                // Estilo Flutter: Actualizar usageCount del ejercicio en el backend
                try {
                    val getEx = service.getEjercicio(bearer, exerciseIdStr)
                    if (getEx.isSuccessful) {
                        val currentEx = getEx.body()
                        val oldUsage = currentEx?.usageCount ?: 0
                        val oldRatingCount = currentEx?.ratingCount ?: 0
                        val oldRatingAvg = currentEx?.ratingAvg ?: "0"

                        val newUsage = oldUsage + 1
                        val updateReq = com.zenmind.kotlin.features.breathing.data.UpdateExerciseRequest(
                            usageCount = newUsage,
                            ratingCount = oldRatingCount,
                            ratingAvg = oldRatingAvg
                        )
                        val putEx = service.updateEjercicio(bearer, exerciseIdStr, updateReq)
                        android.util.Log.d("BreathingSyncWorker", "updateEjercicio PUT code=${putEx.code()}, success=${putEx.isSuccessful}")
                    }
                } catch (ex: Exception) {
                    android.util.Log.e("BreathingSyncWorker", "Error actualizando usageCount: ${ex.message}")
                }

                dao.markSynced(session.id)
                android.util.Log.d("BreathingSyncWorker", "Sesion id=${session.id} synced=1 EXITOSA")
            }
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("BreathingSyncWorker", "Excepcion en sync: ${e.message}", e)
            Result.retry()
        }
    }
}

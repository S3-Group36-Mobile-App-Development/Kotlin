package com.zenmind.kotlin.features.breathing.data

import com.zenmind.kotlin.features.breathing.data.local.BreathingSessionEntity
import com.zenmind.kotlin.features.breathing.data.local.BreathingSyncWorker
import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.zenmind.kotlin.core.network.ApiClient
import com.zenmind.kotlin.core.storage.TokenStorage
import com.zenmind.kotlin.features.breathing.data.local.BreathingExerciseDao
import com.zenmind.kotlin.features.breathing.data.local.BreathingPreferences
import com.zenmind.kotlin.features.breathing.data.local.BreathingSessionDao
import com.zenmind.kotlin.features.breathing.model.BreathingExercise
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

data class CrearSesionRequest(val ejercicioId: Int)
data class SesionResponse(val id: Int)

data class PatronDto (
    val inhalar: Int? = null,
    val sostener: Int? = null,
    val exhalar: Int? = null,
    val sostener2: Int? = null,
    val ciclos: Int? = null
)

data class EjercicioDto(
    val id: String,
    val titulo: String,
    val descripcion: String?,
    val duracion: Int,
    val patron: PatronDto,
    val vibra: Boolean,
    val usageCount: Int = 0,
    val ratingAvg: String = "0",
    val ratingCount: Int = 0
)

interface BreathingApiService {
    @POST("api/v1/respira/sesiones")
    suspend fun crearSesion(
        @Header("Authorization") authorization: String,
        @Body request: CrearSesionRequest
    ): Response<SesionResponse>

    @PATCH("api/v1/respira/sesiones/{id}/completar")
    suspend fun completarSesion(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Response<SesionResponse>

    @GET("api/v1/respira/ejercicios")
    suspend fun getEjercicios(): Response<List<EjercicioDto>>
}


class BreathingRepository(
    private val dao: BreathingSessionDao,
    private val exerciseDao: BreathingExerciseDao,
    private val preferences: BreathingPreferences,
    private val context: Context
) {
    private val service: BreathingApiService by lazy {
        ApiClient.retrofit.create(BreathingApiService::class.java)
    }

    suspend fun getExercises(): List<BreathingExercise> {
        try {
            val response = service.getEjercicios()
            if (response.isSuccessful) {
                response.body()?.let { dtos ->
                    exerciseDao.upsertAll(dtos.map { it.toEntity() })
                }
            }
        } catch (e: Exception) {
            android.util.Log.d("BreathingCache", "Sin red, usando cache: ${e.message}" )
        }
        return exerciseDao.getAll().map { it.toExercise() }
    }

    fun getFavorite(): Int? = preferences.getFavorite()

    fun setFavorite(ejercicioId: Int) = preferences.setFavorite(ejercicioId)

    fun clearFavorite() = preferences.clearFavorite()
    
    /**
     * Registra una sesion completada de forma local y encola la
     * sincronizacion con el backend. La escritura en Room nunca falla;
     * el envio al backend lo hace el worker cuando haya red.
     */
    suspend fun guardarSesion(ejercicioId: Int) {
        val id = dao.insert(
            BreathingSessionEntity(
                ejercicioId = ejercicioId,
                completedAt = System.currentTimeMillis()
            )
        )
        android.util.Log.d("BreathingSync", "Insertado en Room id=$id, total=${dao.countAll()}")
        enqueueSync()
        android.util.Log.d("BreathingSync", "Worker encolado")
    }

    private fun enqueueSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<BreathingSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "breathing_sync",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}

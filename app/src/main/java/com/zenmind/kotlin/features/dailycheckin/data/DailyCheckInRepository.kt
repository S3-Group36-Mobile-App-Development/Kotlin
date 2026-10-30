package com.zenmind.kotlin.features.dailycheckin.data

import android.util.Base64
import android.util.Log
import com.zenmind.kotlin.core.storage.TokenStorage
import com.zenmind.kotlin.features.dailycheckin.data.db.CheckinDao
import com.zenmind.kotlin.features.dailycheckin.data.db.CheckinEntity
import com.zenmind.kotlin.features.dailycheckin.model.Flashcard
import com.zenmind.kotlin.features.dailycheckin.model.Mood
import com.zenmind.kotlin.features.dailycheckin.model.SaveResult
import com.zenmind.kotlin.features.dailycheckin.model.Streak
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import org.json.JSONObject
import kotlin.Long
import kotlin.String

interface DailyCheckInRepository {
    suspend fun getMoods(): List<Mood>
    suspend fun getStreak(): Streak
    suspend fun getTodayCheckInMood(): Mood?
    suspend fun saveCheckIn(mood: Mood, note: String?): SaveResult
    suspend fun getRecommendedFlashcard(mood: Mood): Flashcard?
}

class AlreadyCheckedInException : Exception("Ya registraste tu check-in de hoy.")

class NotLoggedInException : Exception("Inicia sesión para hacer tu check-in.")

class NetworkDailyCheckInRepository(
    private val api: CheckInApiService,
    private val tokenStorage: TokenStorage,
    private val flashcardsCache: FlashcardsCache,
    private val checkinDao: CheckinDao
) : DailyCheckInRepository {
    private fun auth(): String {
        val token = tokenStorage.getAccessToken() ?: throw NotLoggedInException()
        return "Bearer $token"
    }

    override suspend fun getMoods(): List<Mood> =
        api.getMoods().map { Mood(it.id.toLong(), it.nombre) }

    override suspend fun getStreak(): Streak {
        val racha = api.getProfile(auth()).racha
        return Streak(racha?.rachaActualDias ?: 0, racha?.rachaMasLargaDias ?: 0)
    }

    override suspend fun getTodayCheckInMood(): Mood? {
        val utc = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        utc.timeZone = TimeZone.getTimeZone("UTC")
        val today = utc.format(Date())

        val todayCheckIn = api.getCheckIns(auth()).find { it.registradoEn.take(10) == today }
        return todayCheckIn?.let { Mood(it.estadoAnimo.id.toLong(), it.estadoAnimo.nombre) }
    }

    private fun currentUserId(): String {
        val token = tokenStorage.getAccessToken() ?: return GUEST_ID
        return try {
            val payload = token.split(".")[1]
            val json = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
            JSONObject(json).getString("sub")
        } catch (e: Exception) {
            GUEST_ID
        }
    }

    override suspend fun saveCheckIn(mood: Mood, note: String?): SaveResult {
        val guest = tokenStorage.getAccessToken() == null
        val userId = currentUserId()

        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = format.format(Date())

        if (checkinDao.getByDate(today, userId) != null) {
            return SaveResult.ALREADY_REGISTERED
        }

        val id = checkinDao.insertCheckin(
            CheckinEntity(
                date = today,
                moodId = mood.id,
                moodName = mood.name,
                note = note,
                synced = false,
                isGuest = guest,
                userId = userId
            )
        )

        if (guest) return SaveResult.GUEST

        return try {
            val response = api.saveCheckIn(auth(), CheckInRequest(mood.id, note))
            if (response.isSuccessful || response.code() == 409) {
                checkinDao.markSynced(id)
                SaveResult.SYNCED
            } else {
                SaveResult.PENDING
            }
        } catch (e: IOException) {
            SaveResult.PENDING
        }
    }

    override suspend fun getRecommendedFlashcard(mood: Mood): Flashcard? {
        val category = when (mood.name) {
            "Ansioso" -> "Ansiedad"
            "Estresado" -> "Estrés"
            else -> return null
        }
        return try {
            var flashcards = flashcardsCache.getFlashcards(category)
            if (flashcards == null){
                Log.d("FlashcardsCache", "Fetching from Network")
                flashcards = api.getFlashcards(category).map { Flashcard(it.categoria, it.contenido)  }
                if (flashcards.isNotEmpty()){
                    flashcardsCache.saveFlashcards(category, flashcards)
                }
            } else {
                Log.d("FlashcardsCache", "Fetching from the cache")
            }
            flashcards.firstOrNull()
        } catch (e: Exception){
            Log.e("FlashcardsCache", "No se pudo obtener la flashcard de $category", e)
            null
        }
    }

    private companion object {
        const val GUEST_ID = "guest"
    }
}

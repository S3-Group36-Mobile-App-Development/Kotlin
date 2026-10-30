package com.zenmind.kotlin.features.dailycheckin.data

import android.util.Log
import com.zenmind.kotlin.core.storage.TokenStorage
import com.zenmind.kotlin.features.dailycheckin.data.db.CheckinDao
import com.zenmind.kotlin.features.dailycheckin.model.Flashcard
import com.zenmind.kotlin.features.dailycheckin.model.Mood
import com.zenmind.kotlin.features.dailycheckin.model.Streak
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

interface DailyCheckInRepository {
    suspend fun getMoods(): List<Mood>
    suspend fun getStreak(): Streak
    suspend fun getTodayCheckInMood(): Mood?
    suspend fun saveCheckIn(moodId: Long, note: String?)
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

    override suspend fun saveCheckIn(moodId: Long, note: String?) {
        val response = api.saveCheckIn(auth(), CheckInRequest(moodId, note))
        if (response.code() == 409) throw AlreadyCheckedInException()
        if (!response.isSuccessful) throw IOException("HTTP ${response.code()}")
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
}

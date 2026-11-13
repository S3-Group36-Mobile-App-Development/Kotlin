package com.zenmind.kotlin.features.dailycheckin.data

import android.util.Base64
import android.util.Log
import com.zenmind.kotlin.core.storage.TokenStorage
import com.zenmind.kotlin.features.dailycheckin.data.db.CheckinDao
import com.zenmind.kotlin.features.dailycheckin.data.db.CheckinEntity
import com.zenmind.kotlin.features.dailycheckin.data.db.MoodDao
import com.zenmind.kotlin.features.dailycheckin.data.db.MoodEntity
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
import retrofit2.HttpException

interface DailyCheckInRepository {
    fun isGuest(): Boolean
    suspend fun getMoods(): List<Mood>
    suspend fun getStreak(): Streak?
    suspend fun getTodayCheckInMood(): Mood?
    suspend fun isTodayPending(): Boolean
    suspend fun saveCheckIn(mood: Mood, note: String?): SaveResult
    suspend fun syncPending(): Boolean
    fun hasRecommendation(mood: Mood): Boolean
    suspend fun getRecommendedFlashcard(mood: Mood): Flashcard?
}

class OfflineFirstDailyCheckInRepository(
    private val api: CheckInApiService,
    private val tokenStorage: TokenStorage,
    private val flashcardsCache: FlashcardsCache,
    private val checkinDao: CheckinDao,
    private val moodDao: MoodDao
) : DailyCheckInRepository {
    private fun auth(): String =
        "Bearer ${checkNotNull(tokenStorage.getAccessToken())}"


    override fun isGuest(): Boolean = tokenStorage.getAccessToken() == null

    override suspend fun getMoods(): List<Mood> {
        try {
            val remote = api.getMoods()
            if (remote.isNotEmpty()) {
                moodDao.replaceAll(remote.map { MoodEntity(it.id.toLong(), it.nombre, it.orden) })
            }
        } catch (e: Exception) {
            Log.w("DailyCheckIn", "No se pudieron actualizar los estados de ánimo", e)
        }
        return moodDao.getAll().map { Mood(it.id, it.name) }
    }

    override suspend fun getStreak(): Streak? {
        if (isGuest()) return streakFromDates(checkinDao.getDates(GUEST_ID))
        return try {
            val racha = api.getProfile(auth()).racha
            Streak(racha?.rachaActualDias ?: 0, racha?.rachaMasLargaDias ?: 0)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getTodayCheckInMood(): Mood? {
        val userId = currentUserId()
        checkinDao.getByDate(localToday(), userId)?.let { return Mood(it.moodId, it.moodName) }
        if (isGuest()) return null

        return try {
            val today = localToday()
            todayOnServer()?.let {
                val mood = Mood(it.estadoAnimo.id.toLong(), it.estadoAnimo.nombre)
                checkinDao.insertCheckin(
                    CheckinEntity(
                        date = today,
                        moodId = mood.id,
                        moodName = mood.name,
                        note = it.comentario,
                        synced = true,
                        userId = userId
                    )
                )
                mood
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun todayOnServer(): CheckInDto? {
        val today = localToday()
        return api.getCheckIns(auth()).find { toLocalDate(it.registradoEn) == today }
    }

    private suspend fun keepServerCheckIn(id: Long) {
        val server = try {
            todayOnServer()
        } catch (e: HttpException) {
            null
        }
        if (server == null) {
            checkinDao.markSynced(id)
        } else {
            checkinDao.replaceWithServer(id, server.estadoAnimo.id.toLong(), server.estadoAnimo.nombre, server.comentario)
        }
    }

    override suspend fun isTodayPending(): Boolean {
        if (isGuest()) return false
        return checkinDao.getByDate(localToday(), currentUserId())?.synced == false
    }

    override suspend fun syncPending(): Boolean {
        if (isGuest()) return false
        val checkin = checkinDao.getByDate(localToday(), currentUserId()) ?: return false
        if (checkin.synced) return false

        return try {
            val response = api.saveCheckIn(auth(), CheckInRequest(checkin.moodId, checkin.note))
            when {
                response.isSuccessful -> {
                    checkinDao.markSynced(checkin.id)
                    true
                }
                response.code() == 409 -> {
                    keepServerCheckIn(checkin.id)
                    true
                }
                else -> false
            }
        } catch (e: IOException) {
            false
        }
    }

    private fun localToday(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun toLocalDate(isoUtc: String): String? {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val instant = parser.parse(isoUtc.take(19)) ?: return null
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(instant)
        } catch (e: Exception) {
            null
        }
    }

    private fun streakFromDates(dates: List<String>): Streak {
        if (dates.isEmpty()) return Streak(0, 0)
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val days = dates.mapNotNull { parser.parse(it)?.time?.div(DAY_MS) }.distinct().sortedDescending()
        val today = parser.parse(localToday())!!.time / DAY_MS

        var current = 0
        if (days.first() >= today - 1) {
            current = 1
            while (current < days.size && days[current - 1] - days[current] == 1L) current++
        }

        var longest = 1
        var run = 1
        for (i in 1 until days.size) {
            run = if (days[i - 1] - days[i] == 1L) run + 1 else 1
            if (run > longest) longest = run
        }
        return Streak(current, longest)
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
        val userId = currentUserId()
        val today = localToday()

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
                userId = userId
            )
        )

        if (isGuest()) return SaveResult.GUEST

        return try {
            val response = api.saveCheckIn(auth(), CheckInRequest(mood.id, note))
            when {
                response.isSuccessful -> {
                    checkinDao.markSynced(id)
                    SaveResult.SYNCED
                }
                response.code() == 409 -> {
                    keepServerCheckIn(id)
                    SaveResult.ALREADY_REGISTERED
                }
                else -> SaveResult.PENDING
            }
        } catch (e: IOException) {
            SaveResult.PENDING
        }
    }

    private fun categoryFor(mood: Mood): String? = when (mood.name) {
        "Ansioso" -> "Ansiedad"
        "Estresado" -> "Estrés"
        else -> null
    }

    override fun hasRecommendation(mood: Mood): Boolean = categoryFor(mood) != null

    override suspend fun getRecommendedFlashcard(mood: Mood): Flashcard? {
        val category = categoryFor(mood) ?: return null
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
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}

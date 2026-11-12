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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

interface DailyCheckInRepository {
    val syncEvents: SharedFlow<Int>
    fun isGuest(): Boolean
    suspend fun getLocalMoods(): List<Mood>
    suspend fun getLocalTodayMood(): Mood?
    suspend fun prefetch()
    suspend fun getMoods(): List<Mood>
    suspend fun getLocalStreak(): Streak
    suspend fun getStreak(): Streak?
    suspend fun getTodayCheckInMood(): Mood?
    suspend fun isTodayPending(): Boolean
    suspend fun saveCheckIn(mood: Mood, note: String?): SaveResult
    suspend fun syncPending(): Int
    suspend fun getDraft(): String
    suspend fun saveDraft(note: String)
    fun hasRecommendation(mood: Mood): Boolean
    fun getCachedFlashcard(mood: Mood): Flashcard?
    suspend fun getRecommendedFlashcard(mood: Mood): Flashcard?
}

class NotLoggedInException : Exception("Inicia sesión para hacer tu check-in.")

class OfflineFirstDailyCheckInRepository(
    private val api: CheckInApiService,
    private val tokenStorage: TokenStorage,
    private val flashcardsCache: FlashcardsCache,
    private val flashcardFile: FlashcardFileStore,
    private val draftStore: CheckInDraftStore,
    private val checkinDao: CheckinDao,
    private val moodDao: MoodDao
) : DailyCheckInRepository {
    private val _syncEvents = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    override val syncEvents: SharedFlow<Int> = _syncEvents.asSharedFlow()

    private fun auth(): String {
        val token = tokenStorage.getAccessToken() ?: throw NotLoggedInException()
        return "Bearer $token"
    }

    private val syncMutex = Mutex()

    override fun isGuest(): Boolean = tokenStorage.getAccessToken() == null

    override suspend fun getMoods(): List<Mood> {
        return try {
            val moods = api.getMoods().map { Mood(it.id.toLong(), it.nombre) }
            moodDao.upsertAll(moods.map { MoodEntity(it.id, it.name) })
            moods
        } catch (e: Exception) {
            getLocalMoods()
        }
    }

    override suspend fun getLocalMoods(): List<Mood> =
        moodDao.getAll().map { Mood(it.id, it.name) }.ifEmpty { DEFAULT_MOODS }

    override suspend fun getLocalTodayMood(): Mood? =
        checkinDao.getByDate(localToday(), currentUserId())?.let { Mood(it.moodId, it.moodName) }

    override suspend fun prefetch() {
        DEFAULT_MOODS.mapNotNull { categoryFor(it) }.forEach { downloadFlashcard(it) }
        if (isGuest()) return
        getMoods()
        getTodayCheckInMood()
    }

    private suspend fun resolveMoodId(moodId: Long, moodName: String): Long? {
        if (moodId > 0) return moodId
        val known = moodDao.getAll().ifEmpty {
            try {
                getMoods()
                moodDao.getAll()
            } catch (e: Exception) {
                emptyList()
            }
        }
        return known.find { it.name == moodName }?.id
    }

    private fun isDefinitiveRejection(code: Int): Boolean =
        code in 400..499 && code != 401 && code != 408 && code != 429

    override suspend fun getLocalStreak(): Streak =
        streakFromDates(checkinDao.getDates(currentUserId()))

    override suspend fun getStreak(): Streak? {
        if (isGuest()) return getLocalStreak()
        return try {
            val racha = api.getProfile(auth()).racha
            val current = if (isStreakExpired(racha?.fechaUltimoCheckin)) 0 else racha?.rachaActualDias ?: 0
            Streak(current, racha?.rachaMasLargaDias ?: 0)
        } catch (e: Exception) {
            getLocalStreak()
        }
    }

    override suspend fun getTodayCheckInMood(): Mood? {
        val userId = currentUserId()
        checkinDao.getByDate(localToday(), userId)?.let { return Mood(it.moodId, it.moodName) }
        if (isGuest()) return null

        return try {
            val today = localToday()
            val todayCheckIn = api.getCheckIns(auth()).find { toLocalDate(it.registradoEn) == today }
            todayCheckIn?.let {
                val mood = Mood(it.estadoAnimo.id.toLong(), it.estadoAnimo.nombre)
                checkinDao.insertCheckin(
                    CheckinEntity(
                        date = today,
                        moodId = mood.id,
                        moodName = mood.name,
                        note = null,
                        synced = true,
                        isGuest = false,
                        userId = userId
                    )
                )
                mood
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun isTodayPending(): Boolean {
        if (isGuest()) return false
        return checkinDao.getByDate(localToday(), currentUserId())?.synced == false
    }

    override suspend fun syncPending(): Int {
        if (isGuest()) return 0
        return syncMutex.withLock { sendPendingOfToday() }
    }

    private suspend fun sendPendingOfToday(): Int {
        var synced = 0
        for (pending in checkinDao.getPending(currentUserId(), localToday())) {
            try {
                val moodId = resolveMoodId(pending.moodId, pending.moodName) ?: break
                val response = api.saveCheckIn(auth(), CheckInRequest(moodId, pending.note))
                if (response.isSuccessful || response.code() == 409) {
                    checkinDao.markSynced(pending.id)
                    synced++
                } else if (isDefinitiveRejection(response.code())) {
                    checkinDao.markSynced(pending.id)
                }
            } catch (e: IOException) {
                break
            }
        }
        if (synced > 0) _syncEvents.tryEmit(synced)
        return synced
    }

    private fun localToday(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun isStreakExpired(lastCheckIn: String?): Boolean {
        if (lastCheckIn == null) return false
        val utc = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val last = utc.parse(lastCheckIn.take(10))?.time ?: return false
        val today = utc.parse(utc.format(Date()))?.time ?: return false
        return (today - last) / DAY_MS > 1
    }

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
        val guest = tokenStorage.getAccessToken() == null
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
                isGuest = guest,
                userId = userId
            )
        )

        draftStore.save(userId, "")

        if (guest) return SaveResult.GUEST

        return syncMutex.withLock { sendNewCheckIn(id, mood, note) }
    }

    private suspend fun sendNewCheckIn(id: Long, mood: Mood, note: String?): SaveResult {
        if (checkinDao.getByDate(localToday(), currentUserId())?.synced == true) return SaveResult.SYNCED
        val moodId = resolveMoodId(mood.id, mood.name) ?: return SaveResult.PENDING

        return try {
            val response = api.saveCheckIn(auth(), CheckInRequest(moodId, note))
            when {
                response.isSuccessful -> {
                    checkinDao.markSynced(id)
                    SaveResult.SYNCED
                }
                response.code() == 409 -> {
                    checkinDao.markSynced(id)
                    SaveResult.ALREADY_REGISTERED
                }
                isDefinitiveRejection(response.code()) -> {
                    checkinDao.markSynced(id)
                    SaveResult.LOCAL_ONLY
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

    override suspend fun getDraft(): String = draftStore.read(currentUserId())

    override suspend fun saveDraft(note: String) = draftStore.save(currentUserId(), note)

    override fun hasRecommendation(mood: Mood): Boolean = categoryFor(mood) != null

    override fun getCachedFlashcard(mood: Mood): Flashcard? {
        val category = categoryFor(mood) ?: return null
        return flashcardsCache.getFlashcards(category)?.firstOrNull() ?: flashcardFile.read(category)
    }

    override suspend fun getRecommendedFlashcard(mood: Mood): Flashcard? {
        val category = categoryFor(mood) ?: return null
        return getCachedFlashcard(mood) ?: downloadFlashcard(category)
    }

    private suspend fun downloadFlashcard(category: String): Flashcard? {
        return try {
            val flashcards = api.getFlashcards(category).map { Flashcard(it.categoria, it.contenido) }
            if (flashcards.isNotEmpty()) {
                flashcardsCache.saveFlashcards(category, flashcards)
                flashcardFile.save(category, flashcards.first())
            }
            flashcards.firstOrNull()
        } catch (e: Exception) {
            Log.e("FlashcardsCache", "No se pudo obtener la flashcard de $category", e)
            null
        }
    }

    private companion object {
        const val GUEST_ID = "guest"
        const val DAY_MS = 24L * 60 * 60 * 1000

        val DEFAULT_MOODS = listOf(
            Mood(-1, "Feliz"),
            Mood(-2, "Tranquilo"),
            Mood(-3, "Ansioso"),
            Mood(-4, "Triste"),
            Mood(-5, "Estresado")
        )
    }
}

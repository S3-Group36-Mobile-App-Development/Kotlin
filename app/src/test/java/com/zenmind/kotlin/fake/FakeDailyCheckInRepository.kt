package com.zenmind.kotlin.fake

import com.zenmind.kotlin.features.dailycheckin.data.DailyCheckInRepository
import com.zenmind.kotlin.features.dailycheckin.model.Flashcard
import com.zenmind.kotlin.features.dailycheckin.model.Mood
import com.zenmind.kotlin.features.dailycheckin.model.SaveResult
import com.zenmind.kotlin.features.dailycheckin.model.Streak

object FakeCheckInDataSource {
    val anxious = Mood(3, "Ansioso")
    val moods = listOf(Mood(1, "Feliz"), Mood(2, "Tranquilo"), anxious)
    val streak = Streak(current = 3, longest = 7)
    val flashcard = Flashcard("Ansiedad", "Nombra 5 cosas que ves...")
}

class FakeDailyCheckInRepository(
    private val alreadyExists: Boolean = false
) : DailyCheckInRepository {

    var savedMoodId: Long? = null

    override fun isGuest() = false
    override suspend fun getMoods() = FakeCheckInDataSource.moods
    override suspend fun getStreak(): Streak? = FakeCheckInDataSource.streak
    override suspend fun getTodayCheckInMood(): Mood? = if (alreadyExists) FakeCheckInDataSource.anxious else null
    override suspend fun isTodayPending() = false
    override suspend fun syncPending() = 0

    override suspend fun saveCheckIn(mood: Mood, note: String?): SaveResult {
        if (alreadyExists) return SaveResult.ALREADY_REGISTERED
        savedMoodId = mood.id
        return SaveResult.SYNCED
    }

    override suspend fun getRecommendedFlashcard(mood: Mood) = FakeCheckInDataSource.flashcard
}

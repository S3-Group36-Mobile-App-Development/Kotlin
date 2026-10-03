package com.zenmind.kotlin.fake

import com.zenmind.kotlin.features.dailycheckin.data.AlreadyCheckedInException
import com.zenmind.kotlin.features.dailycheckin.data.DailyCheckInRepository
import com.zenmind.kotlin.features.dailycheckin.model.Flashcard
import com.zenmind.kotlin.features.dailycheckin.model.Mood
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

    override suspend fun getMoods() = FakeCheckInDataSource.moods
    override suspend fun getStreak() = FakeCheckInDataSource.streak
    override suspend fun getTodayCheckInMood(): Mood? = null

    override suspend fun saveCheckIn(moodId: Long, note: String?) {
        if (alreadyExists) throw AlreadyCheckedInException()
        savedMoodId = moodId
    }

    override suspend fun getRecommendedFlashcard(mood: Mood) = FakeCheckInDataSource.flashcard
}

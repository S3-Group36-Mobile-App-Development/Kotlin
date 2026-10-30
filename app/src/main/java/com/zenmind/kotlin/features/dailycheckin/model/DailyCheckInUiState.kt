package com.zenmind.kotlin.features.dailycheckin.model

data class DailyCheckInUiState(
    val isLoading: Boolean = false,
    val isGuest: Boolean = false,
    val isOffline: Boolean = false,
    val pendingSync: Boolean = false,
    val moods: List<Mood> = emptyList(),
    val selectedMoodId: Long? = null,
    val note: String = "",
    val streak: Streak? = null,
    val alreadyCheckedInToday: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
    val recommendedFlashcard: Flashcard? = null,
    val isLoadingCard: Boolean = false,
    val savedMessage: String? = null
)

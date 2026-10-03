package com.zenmind.kotlin

import com.zenmind.kotlin.fake.FakeCheckInDataSource
import com.zenmind.kotlin.fake.FakeDailyCheckInRepository
import com.zenmind.kotlin.features.dailycheckin.viewmodel.DailyCheckInViewModel
import com.zenmind.kotlin.rules.TestDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DailyCheckInViewModelTest {
    @get:Rule
    val testDispatcher = TestDispatcherRule()

    @Test
    fun dailyCheckInViewModel_save_blocksAndShowsFlashcard() = runTest {
        val repository = FakeDailyCheckInRepository()
        val viewModel = DailyCheckInViewModel(repository)
        viewModel.onMoodSelected(FakeCheckInDataSource.anxious)
        viewModel.onSave()

        val state = viewModel.uiState.value
        assertEquals(FakeCheckInDataSource.anxious.id, repository.savedMoodId)
        assertTrue(state.alreadyCheckedInToday)
        assertEquals(FakeCheckInDataSource.flashcard, state.recommendedFlashcard)
    }

    @Test
    fun dailyCheckInViewModel_saveTwice_blocksWithMessage() = runTest {
        val viewModel = DailyCheckInViewModel(FakeDailyCheckInRepository(alreadyExists = true))
        viewModel.onMoodSelected(FakeCheckInDataSource.anxious)
        viewModel.onSave()
        assertTrue(viewModel.uiState.value.alreadyCheckedInToday)
    }
}

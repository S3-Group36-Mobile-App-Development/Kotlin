package com.zenmind.kotlin

import com.zenmind.kotlin.fake.FakeDataSource
import com.zenmind.kotlin.fake.FakeErrorHomeRepository
import com.zenmind.kotlin.fake.FakeNetworkHomeRepository
import com.zenmind.kotlin.features.home.model.HomeUiState
import com.zenmind.kotlin.features.home.viewmodel.HomeViewModel
import com.zenmind.kotlin.rules.TestDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule
    val testDispatcher = TestDispatcherRule()

    @Test
    fun homeViewModel_getHomeData_verifyHomeUiStateSuccess() =
        runTest {
            val homeViewModel = HomeViewModel(
                repository = FakeNetworkHomeRepository()
            )
            assertEquals(
                HomeUiState.Success(FakeDataSource.homeData),
                homeViewModel.uiState.value
            )
        }

    @Test
    fun homeViewModel_getHomeData_verifyHomeUiStateError() =
        runTest {
            val homeViewModel = HomeViewModel(
                repository = FakeErrorHomeRepository()
            )
            assertTrue(homeViewModel.uiState.value is HomeUiState.Error)
        }
}

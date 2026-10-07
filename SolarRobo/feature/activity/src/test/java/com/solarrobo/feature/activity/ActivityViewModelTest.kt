package com.solarrobo.feature.activity

import com.solarrobo.core.contracts.ActivityEvent
import com.solarrobo.core.contracts.ActivityType
import com.solarrobo.feature.activity.mock.FakeActivityRepository
import com.solarrobo.feature.activity.presentation.ActivityViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ActivityViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = ActivityViewModel(FakeActivityRepository())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun defaultList_containsEvents() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.events.isNotEmpty())
    }

    @Test
    fun filter_showsOnlyMatchingType() = runTest {
        viewModel.setFilter(ActivityType.SAFETY)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.events.all { it.type == ActivityType.SAFETY })
    }

    @Test
    fun emptyFilter_showsNoEvents() = runTest {
        viewModel.setFilter(ActivityType.AI)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.events.isEmpty())
    }

    @Test
    fun recordEvent_addsNewEventToTop() = runTest {
        viewModel.setFilter(null)
        advanceUntilIdle()
        val newEvent = ActivityEvent(
            id = "new-event",
            type = ActivityType.SYSTEM,
            title = "System Refresh",
            detail = "Background sync complete",
            timestamp = System.currentTimeMillis()
        )
        val repo = FakeActivityRepository()
        repo.recordEvent(newEvent)
        val freshViewModel = ActivityViewModel(repo)
        freshViewModel.setFilter(null)
        advanceUntilIdle()
        assertEquals(newEvent.id, freshViewModel.uiState.value.events.first().id)
    }
}

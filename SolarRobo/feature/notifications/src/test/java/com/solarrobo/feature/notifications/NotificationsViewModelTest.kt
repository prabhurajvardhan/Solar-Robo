package com.solarrobo.feature.notifications

import com.solarrobo.core.contracts.NotificationEvent
import com.solarrobo.feature.notifications.domain.NotificationsRepository
import com.solarrobo.feature.notifications.mock.FakeNotificationsRepository
import com.solarrobo.feature.notifications.mock.prototypeNotifications
import com.solarrobo.feature.notifications.presentation.NotificationsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun prototypeRepository_hasDeterministicPrioritiesOrderingAndReadStates() = runTest(dispatcher) {
        val repository = FakeNotificationsRepository()
        val notifications = repository.getNotifications().first()

        assertEquals(6, notifications.size)
        assertEquals(
            notifications.sortedByDescending { it.timestamp },
            notifications
        )
        assertEquals(4, notifications.map { it.priority }.toSet().size)
        assertTrue(notifications.any { it.isRead })
        assertTrue(notifications.any { !it.isRead })
        assertTrue(notifications.all { it.body.startsWith("Prototype scenario:") })
        assertEquals(4, repository.getUnreadCount().first())
    }

    @Test
    fun emptyRepository_producesEmptyInboxAndZeroUnread() = runTest(dispatcher) {
        val repository = FakeNotificationsRepository(emptyList())
        val viewModel = NotificationsViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()

        assertTrue(viewModel.uiState.value.notifications.isEmpty())
        assertEquals(0, viewModel.uiState.value.unreadCount)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun viewModel_startsInLoadingStateUntilInboxEmits() = runTest(dispatcher) {
        val viewModel = NotificationsViewModel(FakeNotificationsRepository())

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun viewModel_marksNotificationReadAndUpdatesUnreadBadge() = runTest(dispatcher) {
        val repository = FakeNotificationsRepository()
        val viewModel = NotificationsViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()

        val unread = prototypeNotifications.first { !it.isRead }
        assertEquals(4, viewModel.uiState.value.unreadCount)

        viewModel.markRead(unread.id)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.notifications.first { it.id == unread.id }.isRead)
        assertEquals(3, viewModel.uiState.value.unreadCount)
    }

    @Test
    fun viewModel_surfacesLocalStorageError() = runTest(dispatcher) {
        val viewModel = NotificationsViewModel(FailingNotificationsRepository())
        backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isOffline)
    }
}

private class FailingNotificationsRepository : NotificationsRepository {
    override fun getNotifications(): Flow<List<NotificationEvent>> = flow {
        throw IllegalArgumentException("Notification storage is unavailable.")
    }

    override fun getUnreadCount(): Flow<Int> = flowOf(0)

    override fun getErrorMessage(): Flow<String?> = flowOf(null)

    override suspend fun ingest(event: NotificationEvent) = Unit

    override suspend fun markAsRead(id: String) = Unit
}

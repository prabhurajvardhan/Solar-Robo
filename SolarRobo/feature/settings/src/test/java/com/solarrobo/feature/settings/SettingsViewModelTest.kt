package com.solarrobo.feature.settings

import com.solarrobo.core.common.InProcessSettingsChangedEventBus
import com.solarrobo.core.contracts.SettingsChangedEvent
import com.solarrobo.core.storage.SecurePreferencesStore
import com.solarrobo.feature.settings.data.SettingsRepositoryImpl
import com.solarrobo.feature.settings.domain.SettingsPatch
import com.solarrobo.feature.settings.domain.SettingsRepository
import com.solarrobo.feature.settings.domain.UserSettings
import com.solarrobo.feature.settings.mock.FakeSettingsRepository
import com.solarrobo.feature.settings.presentation.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
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
    fun defaultSettings_areLoaded() = runTest(dispatcher) {
        val viewModel = SettingsViewModel(FakeSettingsRepository())
        runCurrent()

        assertEquals(UserSettings(), viewModel.uiState.value.settings)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isOffline)
    }

    @Test
    fun updateAndReset_refreshAllPreferenceValues() = runTest(dispatcher) {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)
        runCurrent()

        viewModel.update(
            SettingsPatch(
                darkTheme = false,
                notificationsEnabled = false,
                highWindAlertsEnabled = false,
                voiceFeedbackEnabled = true,
                temperatureUnitCelsius = false
            )
        )
        advanceUntilIdle()

        assertEquals(
            UserSettings(
                darkTheme = false,
                notificationsEnabled = false,
                highWindAlertsEnabled = false,
                voiceFeedbackEnabled = true,
                temperatureUnitCelsius = false
            ),
            viewModel.uiState.value.settings
        )

        viewModel.reset()
        advanceUntilIdle()
        assertEquals(UserSettings(), viewModel.uiState.value.settings)
    }

    @Test
    fun viewModel_surfacesUpdateFailure() = runTest(dispatcher) {
        val repository = FakeSettingsRepository().apply {
            updateError = IOException("Encrypted preferences are unavailable.")
        }
        val viewModel = SettingsViewModel(repository)
        runCurrent()

        viewModel.update(SettingsPatch(darkTheme = false))
        advanceUntilIdle()

        assertEquals(
            "Encrypted preferences are unavailable.",
            viewModel.uiState.value.errorMessage
        )
        assertNotNull(viewModel.uiState.value.settings)
    }

    @Test
    fun viewModel_surfacesLoadFailureAndCanRetry() = runTest(dispatcher) {
        val viewModel = SettingsViewModel(FailingLoadRepository())
        runCurrent()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Preferences are unreadable.", viewModel.uiState.value.errorMessage)

        viewModel.retry()
        runCurrent()
        assertEquals("Preferences are unreadable.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun repository_persistsPatchAndPublishesChangedEvent() = runTest {
        val store = InMemorySecurePreferencesStore()
        val eventBus = InProcessSettingsChangedEventBus()
        val repository = SettingsRepositoryImpl(store, eventBus)
        val event = async { eventBus.events.first() }

        assertEquals(UserSettings(), repository.load().first())
        assertTrue(repository.update(SettingsPatch(darkTheme = false)).isSuccess)
        assertFalse(repository.load().first().darkTheme)
        assertTrue(event.await().timestamp > 0L)

        assertTrue(repository.reset().isSuccess)
        assertEquals(UserSettings(), repository.load().first())
    }

    @Test
    fun repository_returnsStorageFailureWithoutPublishingEvent() = runTest {
        val eventBus = InProcessSettingsChangedEventBus()
        var receivedEvent: SettingsChangedEvent? = null
        val collector = backgroundScope.async {
            eventBus.events.collect { receivedEvent = it }
        }
        runCurrent()
        val repository = SettingsRepositoryImpl(
            InMemorySecurePreferencesStore().apply { updateFailure = IOException("Disk unavailable.") },
            eventBus
        )

        val result = repository.update(SettingsPatch(notificationsEnabled = false))
        runCurrent()

        assertTrue(result.isFailure)
        assertEquals(null, receivedEvent)
        collector.cancel()
    }
}

private class FailingLoadRepository : SettingsRepository {
    override fun load(): Flow<UserSettings> = flow {
        throw IOException("Preferences are unreadable.")
    }

    override suspend fun update(patch: SettingsPatch): Result<Unit> = Result.success(Unit)

    override suspend fun reset(): Result<Unit> = Result.success(Unit)
}

private class InMemorySecurePreferencesStore : SecurePreferencesStore {
    private val values = MutableStateFlow<Map<String, String>>(emptyMap())
    var updateFailure: IOException? = null

    override val preferences: Flow<Map<String, String>> = values.asStateFlow()

    override suspend fun update(transform: (Map<String, String>) -> Map<String, String>) {
        updateFailure?.let { throw it }
        values.value = transform(values.value)
    }

}

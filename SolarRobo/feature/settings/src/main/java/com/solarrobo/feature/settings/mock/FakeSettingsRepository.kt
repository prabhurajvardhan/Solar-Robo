package com.solarrobo.feature.settings.mock

import com.solarrobo.feature.settings.domain.SettingsPatch
import com.solarrobo.feature.settings.domain.SettingsRepository
import com.solarrobo.feature.settings.domain.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSettingsRepository(
    initialSettings: UserSettings = UserSettings()
) : SettingsRepository {
    private val settings = MutableStateFlow(initialSettings)
    var updateError: Throwable? = null

    override fun load(): Flow<UserSettings> = settings.asStateFlow()

    override suspend fun update(patch: SettingsPatch): Result<Unit> {
        updateError?.let { return Result.failure(it) }
        settings.value = settings.value.copy(
            darkTheme = patch.darkTheme ?: settings.value.darkTheme,
            notificationsEnabled = patch.notificationsEnabled ?: settings.value.notificationsEnabled,
            highWindAlertsEnabled =
                patch.highWindAlertsEnabled ?: settings.value.highWindAlertsEnabled,
            voiceFeedbackEnabled = patch.voiceFeedbackEnabled ?: settings.value.voiceFeedbackEnabled,
            temperatureUnitCelsius =
                patch.temperatureUnitCelsius ?: settings.value.temperatureUnitCelsius
        )
        return Result.success(Unit)
    }

    override suspend fun reset(): Result<Unit> {
        updateError?.let { return Result.failure(it) }
        settings.value = UserSettings()
        return Result.success(Unit)
    }
}

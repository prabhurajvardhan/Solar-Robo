package com.solarrobo.feature.settings.domain

import kotlinx.coroutines.flow.Flow

data class UserSettings(
    val darkTheme: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val highWindAlertsEnabled: Boolean = true,
    val voiceFeedbackEnabled: Boolean = false,
    val temperatureUnitCelsius: Boolean = true
)

data class SettingsPatch(
    val darkTheme: Boolean? = null,
    val notificationsEnabled: Boolean? = null,
    val highWindAlertsEnabled: Boolean? = null,
    val voiceFeedbackEnabled: Boolean? = null,
    val temperatureUnitCelsius: Boolean? = null
)

interface SettingsRepository {
    fun load(): Flow<UserSettings>
    suspend fun update(patch: SettingsPatch): Result<Unit>
    suspend fun reset(): Result<Unit>
}

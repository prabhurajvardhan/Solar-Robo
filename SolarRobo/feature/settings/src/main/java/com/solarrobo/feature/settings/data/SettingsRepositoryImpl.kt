package com.solarrobo.feature.settings.data

import com.solarrobo.core.common.SettingsChangedEventBus
import com.solarrobo.core.contracts.SettingsChangedEvent
import com.solarrobo.core.storage.SecurePreferencesStore
import com.solarrobo.feature.settings.domain.SettingsPatch
import com.solarrobo.feature.settings.domain.SettingsRepository
import com.solarrobo.feature.settings.domain.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val preferencesStore: SecurePreferencesStore,
    private val eventBus: SettingsChangedEventBus
) : SettingsRepository {
    override fun load(): Flow<UserSettings> = preferencesStore.preferences.map { preferences ->
        UserSettings(
            darkTheme = preferences.boolean(DARK_THEME, UserSettings().darkTheme),
            notificationsEnabled = preferences.boolean(
                NOTIFICATIONS_ENABLED,
                UserSettings().notificationsEnabled
            ),
            highWindAlertsEnabled = preferences.boolean(
                HIGH_WIND_ALERTS_ENABLED,
                UserSettings().highWindAlertsEnabled
            ),
            voiceFeedbackEnabled = preferences.boolean(
                VOICE_FEEDBACK_ENABLED,
                UserSettings().voiceFeedbackEnabled
            ),
            temperatureUnitCelsius = preferences.boolean(
                TEMPERATURE_UNIT_CELSIUS,
                UserSettings().temperatureUnitCelsius
            )
        )
    }

    override suspend fun update(patch: SettingsPatch): Result<Unit> = persist {
        val current = toSettings()
        current.copy(
            darkTheme = patch.darkTheme ?: current.darkTheme,
            notificationsEnabled = patch.notificationsEnabled ?: current.notificationsEnabled,
            highWindAlertsEnabled = patch.highWindAlertsEnabled ?: current.highWindAlertsEnabled,
            voiceFeedbackEnabled = patch.voiceFeedbackEnabled ?: current.voiceFeedbackEnabled,
            temperatureUnitCelsius =
                patch.temperatureUnitCelsius ?: current.temperatureUnitCelsius
        ).toPreferences()
    }

    override suspend fun reset(): Result<Unit> = persist { emptyMap() }

    private suspend fun persist(
        transform: Map<String, String>.() -> Map<String, String>
    ): Result<Unit> = try {
        preferencesStore.update { current -> current.transform() }
        eventBus.publish(SettingsChangedEvent(System.currentTimeMillis()))
        Result.success(Unit)
    } catch (error: IOException) {
        Result.failure(error)
    }

    private fun Map<String, String>.toSettings() = UserSettings(
        darkTheme = boolean(DARK_THEME, UserSettings().darkTheme),
        notificationsEnabled = boolean(NOTIFICATIONS_ENABLED, UserSettings().notificationsEnabled),
        highWindAlertsEnabled = boolean(
            HIGH_WIND_ALERTS_ENABLED,
            UserSettings().highWindAlertsEnabled
        ),
        voiceFeedbackEnabled = boolean(VOICE_FEEDBACK_ENABLED, UserSettings().voiceFeedbackEnabled),
        temperatureUnitCelsius = boolean(
            TEMPERATURE_UNIT_CELSIUS,
            UserSettings().temperatureUnitCelsius
        )
    )

    private fun UserSettings.toPreferences() = mapOf(
        DARK_THEME to darkTheme.toString(),
        NOTIFICATIONS_ENABLED to notificationsEnabled.toString(),
        HIGH_WIND_ALERTS_ENABLED to highWindAlertsEnabled.toString(),
        VOICE_FEEDBACK_ENABLED to voiceFeedbackEnabled.toString(),
        TEMPERATURE_UNIT_CELSIUS to temperatureUnitCelsius.toString()
    )

    private fun Map<String, String>.boolean(key: String, default: Boolean): Boolean =
        get(key)?.toBooleanStrictOrNull()
            ?: if (containsKey(key)) {
                throw IllegalArgumentException("Invalid stored setting: $key")
            } else {
                default
            }

    private companion object {
        const val DARK_THEME = "dark_theme"
        const val NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val HIGH_WIND_ALERTS_ENABLED = "high_wind_alerts_enabled"
        const val VOICE_FEEDBACK_ENABLED = "voice_feedback_enabled"
        const val TEMPERATURE_UNIT_CELSIUS = "temperature_unit_celsius"
    }
}

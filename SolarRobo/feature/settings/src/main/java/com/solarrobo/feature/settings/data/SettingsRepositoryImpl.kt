package com.solarrobo.feature.settings.data

import com.solarrobo.core.common.SettingsChangedEventBus
import com.solarrobo.core.contracts.SettingsChangedEvent
import com.solarrobo.core.storage.SecurePreferencesStore
import com.solarrobo.feature.settings.domain.SettingsPatch
import com.solarrobo.feature.settings.domain.SettingsRepository
import com.solarrobo.feature.settings.domain.UserSettings
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val preferencesStore: SecurePreferencesStore,
    private val eventBus: SettingsChangedEventBus
) : SettingsRepository {
    override fun load(): Flow<UserSettings> = preferencesStore.preferences.map { it.toSettings() }

    override suspend fun update(patch: SettingsPatch): Result<Unit> = persist {
        val current = toSettings()
        val next = current.copy(
            darkTheme = patch.darkTheme ?: current.darkTheme,
            notificationsEnabled = patch.notificationsEnabled ?: current.notificationsEnabled,
            highWindAlertsEnabled = patch.highWindAlertsEnabled ?: current.highWindAlertsEnabled,
            voiceFeedbackEnabled = patch.voiceFeedbackEnabled ?: current.voiceFeedbackEnabled,
            temperatureUnitCelsius = patch.temperatureUnitCelsius ?: current.temperatureUnitCelsius,
            latitudeDeg = patch.latitudeDeg ?: current.latitudeDeg,
            longitudeDeg = patch.longitudeDeg ?: current.longitudeDeg,
            travelLimitDeg = patch.travelLimitDeg ?: current.travelLimitDeg
        )
        require(next.latitudeDeg == null || next.latitudeDeg in -90.0..90.0) { "Latitude must be between -90 and 90 degrees." }
        require(next.longitudeDeg == null || next.longitudeDeg in -180.0..180.0) { "Longitude must be between -180 and 180 degrees." }
        require(next.travelLimitDeg.isFinite() && next.travelLimitDeg in 1f..70f) { "Travel limit must be between 1 and 70 degrees." }
        next.toPreferences()
    }

    override suspend fun reset(): Result<Unit> = persist { emptyMap() }

    private suspend fun persist(transform: Map<String, String>.() -> Map<String, String>): Result<Unit> = try {
        preferencesStore.update { current -> current.transform() }
        eventBus.publish(SettingsChangedEvent(System.currentTimeMillis()))
        Result.success(Unit)
    } catch (error: IOException) {
        Result.failure(error)
    } catch (error: IllegalArgumentException) {
        Result.failure(error)
    }

    private fun Map<String, String>.toSettings() = UserSettings(
        darkTheme = boolean(DARK_THEME, UserSettings().darkTheme),
        notificationsEnabled = boolean(NOTIFICATIONS_ENABLED, UserSettings().notificationsEnabled),
        highWindAlertsEnabled = boolean(HIGH_WIND_ALERTS_ENABLED, UserSettings().highWindAlertsEnabled),
        voiceFeedbackEnabled = boolean(VOICE_FEEDBACK_ENABLED, UserSettings().voiceFeedbackEnabled),
        temperatureUnitCelsius = boolean(TEMPERATURE_UNIT_CELSIUS, UserSettings().temperatureUnitCelsius),
        latitudeDeg = double(LATITUDE_DEG),
        longitudeDeg = double(LONGITUDE_DEG),
        travelLimitDeg = get(TRAVEL_LIMIT_DEG)?.toFloatOrNull()?.takeIf { it.isFinite() && it in 1f..70f }
            ?: UserSettings().travelLimitDeg
    )

    private fun UserSettings.toPreferences() = mapOf(
        DARK_THEME to darkTheme.toString(),
        NOTIFICATIONS_ENABLED to notificationsEnabled.toString(),
        HIGH_WIND_ALERTS_ENABLED to highWindAlertsEnabled.toString(),
        VOICE_FEEDBACK_ENABLED to voiceFeedbackEnabled.toString(),
        TEMPERATURE_UNIT_CELSIUS to temperatureUnitCelsius.toString(),
        TRAVEL_LIMIT_DEG to travelLimitDeg.toString()
    ) + listOfNotNull(
        latitudeDeg?.let { LATITUDE_DEG to it.toString() },
        longitudeDeg?.let { LONGITUDE_DEG to it.toString() }
    ).toMap()

    private fun Map<String, String>.boolean(key: String, default: Boolean): Boolean =
        get(key)?.toBooleanStrictOrNull() ?: if (containsKey(key)) {
            throw IllegalArgumentException("Invalid stored setting: $key")
        } else default

    private fun Map<String, String>.double(key: String): Double? = get(key)?.toDoubleOrNull()

    private companion object {
        const val DARK_THEME = "dark_theme"
        const val NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val HIGH_WIND_ALERTS_ENABLED = "high_wind_alerts_enabled"
        const val VOICE_FEEDBACK_ENABLED = "voice_feedback_enabled"
        const val TEMPERATURE_UNIT_CELSIUS = "temperature_unit_celsius"
        const val TRAVEL_LIMIT_DEG = "tracker_travel_limit_deg"
        const val LATITUDE_DEG = "tracker_latitude_deg"
        const val LONGITUDE_DEG = "tracker_longitude_deg"
    }
}

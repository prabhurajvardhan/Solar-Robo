package com.solarrobo.app.di

import com.solarrobo.core.camera.CameraXFrameSource
import com.solarrobo.core.device.Esp32WifiDevice
import com.solarrobo.core.tracker.ControllerTuning
import com.solarrobo.core.tracker.TrackerController
import com.solarrobo.core.tracker.TrackerStateStore
import com.solarrobo.core.contracts.TrackerEngine
import com.solarrobo.core.contracts.TrackerLogSink
import com.solarrobo.feature.settings.domain.SettingsRepository
import com.solarrobo.app.weather.WeatherRuntime
import javax.inject.Inject
import javax.inject.Singleton
import java.time.Clock
import kotlinx.coroutines.flow.first

@Singleton
class TrackerRuntimeFactory @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val device: Esp32WifiDevice,
    private val camera: CameraXFrameSource,
    private val stateStore: TrackerStateStore,
    private val logSink: TrackerLogSink,
    private val safety: com.solarrobo.core.tracker.TrackerSafety,
    private val weatherRuntime: WeatherRuntime
) {
    suspend fun create(): TrackerEngine {
        val settings = settingsRepository.load().first()
        val latitude = settings.latitudeDeg ?: error("Set the tracker latitude in Settings before starting")
        val longitude = settings.longitudeDeg ?: error("Set the tracker longitude in Settings before starting")
        device.connect().getOrElse { throw it }
        weatherRuntime.attachTo(camera)
        return TrackerController(
            motor = device.createMotorAdapter(),
            frames = camera,
            clock = Clock.systemDefaultZone(),
            tuning = ControllerTuning(
                latitudeDeg = latitude,
                longitudeDeg = longitude,
                thetaLimitDeg = settings.travelLimitDeg.toDouble()
            ),
            store = stateStore,
            safety = safety,
            logSink = logSink
        )
    }
}

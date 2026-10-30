package com.solarrobo.core.contracts

import kotlinx.coroutines.flow.Flow

/** Link state of the local ESP32 access point. */
enum class LinkState { CONNECTED, CONNECTING, OFFLINE }
enum class AngleSource { CAMERA_LOCKED, DEAD_RECKONED }
enum class WindRisk { CALM, GUSTY, STORM_RISK, UNKNOWN }
enum class TrackerFault { JAM, LINK_LOST, CAMERA_UNAVAILABLE, CALIBRATION_REQUIRED, LIMIT_REACHED, MOTOR_BUSY }

data class TrackerSnapshot(
    val link: LinkState = LinkState.OFFLINE,
    val angleSource: AngleSource = AngleSource.DEAD_RECKONED,
    val pointingErrorDeg: Float? = null,
    val sunAzimuthDeg: Float? = null,
    val sunElevationDeg: Float? = null,
    val estimatedPanelAngleDeg: Float = 0f,
    val targetAngleDeg: Float = 0f,
    val faults: List<TrackerFault> = emptyList(),
    val calibrated: Boolean = false,
    val timestamp: Long = 0L
)

data class SkyObservation(
    val sunVisible: Boolean,
    val sunOffsetDeg: Float?,
    val jitterPx: Float?,
    val cloudFraction: Float?,
    val brightness: Float?,
    val windRisk: WindRisk,
    val timestamp: Long
)

data class EnergyEstimate(
    val estimatedRelativeOutput: Float,
    val isEstimate: Boolean = true,
    val timestamp: Long
)

data class CalibrationStep(val name: String, val progress: Float, val message: String, val complete: Boolean = false)

enum class LightFollowPhase { IDLE, SEARCHING, LEARNING, LOCKED, MOVING, LOST, END_OF_TRAVEL, ERROR }

data class LightFollowSnapshot(
    val phase: LightFollowPhase = LightFollowPhase.IDLE,
    val lightX: Float? = null,
    val lightY: Float? = null,
    val areaPixels: Int? = null,
    val errorPx: Float? = null,
    val errorDeg: Float? = null,
    val moves: Int = 0,
    val pxPerForwardSecond: Float? = null,
    val message: String? = null,
    val darkFilterWarning: Boolean = false
)

/** Tracking lifecycle boundary exposed to the app; all physical actions remain SafetyGate-owned. */
interface TrackerEngine {
    fun observeState(): Flow<TrackerSnapshot>
    fun observeSky(): Flow<SkyObservation>
    suspend fun start()
    suspend fun stop()
    /** Move by a small signed angle increment; implementations must enforce safety and travel limits. */
    suspend fun jog(deltaDeg: Float)
    fun observeLightFollow(): Flow<LightFollowSnapshot>
    suspend fun startLightFollow()
    suspend fun stopLightFollow()
    fun calibrate(): Flow<CalibrationStep>
    suspend fun setHome()
    suspend fun zeroCamera()
    suspend fun resetFault()
}

package com.solarrobo.core.tracker

import com.solarrobo.core.contracts.AngleSource
import com.solarrobo.core.contracts.LinkState
import com.solarrobo.core.contracts.SkyObservation
import com.solarrobo.core.contracts.SafetyContext
import com.solarrobo.core.contracts.TrackerLogRecord
import com.solarrobo.core.contracts.TrackerLogSink
import com.solarrobo.core.contracts.TrackerEngine
import com.solarrobo.core.contracts.TrackerFault
import com.solarrobo.core.contracts.TrackerSnapshot
import com.solarrobo.core.contracts.WindRisk
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Firmware electrical direction; calibration maps this to east/west panel tilt. */
enum class MoveDirection(val sign: Int) { FORWARD(1), REVERSE(-1) }

class MotorLimitReachedException : Exception("Firmware clamped the move at the configured travel limit")

/** Implementations must only return after the move has completed; never return an HTTP-accepted duration. */
interface Motor {
    fun updateCalibrationSpeed(degreesPerSecond: Double) {}
    suspend fun move(direction: MoveDirection, milliseconds: Long): Long
    suspend fun status(): MotorStatus
    suspend fun stop()
    suspend fun home(): Boolean
    suspend fun sync(thetaDeg: Double)
    suspend fun setLimit(minThetaDeg: Double, maxThetaDeg: Double)
}

data class MotorStatus(val connected: Boolean, val thetaDeg: Double? = null, val busy: Boolean = false)

fun interface FrameSource { suspend fun nextFrame(): RgbFrame? }

/** Called immediately before every physical operation; implementations own actuator policy/interlocks. */
fun interface TrackerSafety { suspend fun authorize(operation: String, context: SafetyContext): Boolean }

interface TrackerStateStore {
    suspend fun load(): PersistedTrackerState?
    suspend fun save(state: PersistedTrackerState)
}

data class PersistedTrackerState(
    val thetaDeg: Double = 0.0,
    val thetaPerSecond: Double = 3.0,
    val calibrated: Boolean = false,
    val lastTargetDeg: Double = 0.0,
    val signX: Int = 1,
    val cameraBiasDeg: Double = 0.0,
    val homeDeclared: Boolean = false,
    val windHold: Boolean = false,
    val jammed: Boolean = false,
    val faultCodes: Set<String> = emptySet()
)

fun interface ControllerSleeper { suspend fun sleep(duration: Duration) }

data class ControllerTuning(
    val latitudeDeg: Double,
    val longitudeDeg: Double,
    val cycle: Duration = Duration.ofSeconds(60),
    val nightSleep: Duration = Duration.ofSeconds(600),
    val calibrationPause: Duration = Duration.ofMinutes(6),
    val nightStowElevationDeg: Double = 8.0,
    val thetaLimitDeg: Double = 70.0,
    val deadbandDeg: Double = 1.0,
    val maxMove: Duration = Duration.ofSeconds(6),
    val jitterThresholdPx: Float = 2.5f,
    val cameraHorizontalFovDeg: Double = 65.0,
    val jitterFrames: Int = 4,
    val jitterFrameSpacing: Duration = Duration.ofMillis(500),
    val windEngageCycles: Int = 2,
    val windReleaseCycles: Int = 10,
    val cameraRehomeAfterMisses: Int = 3,
    val cameraRehomeInterval: Duration = Duration.ofHours(24),
    val jamExpectedFraction: Double = 0.30,
    val jamRetries: Int = 2
) {
    init {
        require(latitudeDeg in -90.0..90.0 && longitudeDeg in -180.0..180.0)
        require(cycle.toMillis() > 0 && nightSleep.toMillis() > 0 && maxMove.toMillis() in 1..6_000)
        require(thetaLimitDeg in 1.0..70.0 && deadbandDeg >= 0.0)
        require(cameraHorizontalFovDeg in 1.0..180.0)
        require(jitterFrames >= 2 && jitterFrameSpacing.toMillis() > 0)
        require(windEngageCycles > 0 && windReleaseCycles > 0 && cameraRehomeAfterMisses > 0)
        require(jamExpectedFraction in 0.0..1.0 && jamRetries > 0)
    }
}

/** Sensorless, east-positive single-axis controller. Call [start] to run its fixed-period loop. */
class TrackerController(
    private val motor: Motor,
    private val frames: FrameSource,
    private val clock: Clock,
    private val tuning: ControllerTuning,
    private val store: TrackerStateStore,
    private val safety: TrackerSafety,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val sleeper: ControllerSleeper = ControllerSleeper { delay(it.toMillis()) },
    private val logSink: TrackerLogSink? = null
) : TrackerEngine {
    private var theta = 0.0
    private var thetaPerSecond = 3.0
    private var calibrated = false
    private var signX = 1
    private var cameraBiasDeg = 0.0
    private var homeDeclared = false
    private var target = 0.0
    private var stateLoaded = false
    private var angleSource = AngleSource.DEAD_RECKONED
    private var linkState = LinkState.OFFLINE
    private var lastStatusAtMs = 0L
    private var lastPruneAtMs = 0L
    private var faults = mutableSetOf<TrackerFault>()
    private var restoredFaults = emptySet<TrackerFault>()
    private var lastRehome: Instant? = null
    private var cameraMisses = 0
    private var gustCycles = 0
    private var calmCycles = 0
    private var windHold = false
    private var jamStreak = 0
    private var job: Job? = null
    private val sky = MutableStateFlow(SkyObservation(false, null, null, null, null, WindRisk.UNKNOWN, nowMs()))
    private val state = MutableStateFlow(snapshot())

    override fun observeState(): StateFlow<TrackerSnapshot> = state.asStateFlow()
    override fun observeSky(): StateFlow<SkyObservation> = sky.asStateFlow()

    override suspend fun start() {
        if (job?.isActive == true) return
        ensureLoaded()
        val initialStatus = motor.status()
        linkState = if (initialStatus.connected) LinkState.CONNECTED else LinkState.OFFLINE
        lastStatusAtMs = nowMs()
        if (!initialStatus.connected) {
            faults.add(TrackerFault.LINK_LOST)
            publish()
            return
        }
        if (safe("set-limits") { motor.setLimit(-tuning.thetaLimitDeg, tuning.thetaLimitDeg) } == null) {
            failLink(); return
        }
        faults.addAll(restoredFaults.filter { it != TrackerFault.LINK_LOST })
        restoredFaults = emptySet()
        publish()
        job = scope.launch {
            while (isActive) {
                val began = clock.millis()
                try { cycleOnce() } catch (cancel: CancellationException) { throw cancel } catch (_: Exception) {
                    faults.add(TrackerFault.LINK_LOST)
                    stopMotor()
                    publish()
                }
                val elapsed = (clock.millis() - began).coerceAtLeast(0)
                val elevation = state.value.sunElevationDeg?.toDouble() ?: 0.0
                val waitMs = (if (elevation >= tuning.nightStowElevationDeg) tuning.cycle.toMillis() else tuning.nightSleep.toMillis()) - elapsed
                if (waitMs > 0) sleeper.sleep(Duration.ofMillis(waitMs))
            }
        }
    }

    /** A single deterministic control pass; useful to host the loop externally or test with fakes. */
    suspend fun cycleOnce() {
        ensureLoaded()
        val instant = clock.instant()
        val position = SolarMath.position(tuning.latitudeDeg, tuning.longitudeDeg, instant)
        val rawTarget = SolarMath.trackerAngle(position.azimuthDeg, position.elevationDeg)
        val isNight = position.elevationDeg < tuning.nightStowElevationDeg
        target = if (isNight) 0.0 else rawTarget.coerceIn(-tuning.thetaLimitDeg, tuning.thetaLimitDeg)
        val sampled = sampleJitterAndSun()
        val jitter = sampled.first
        val blob = sampled.second
        updateWind(jitter)
        val visible = blob != null && !isNight
        sky.value = SkyObservation(visible, if (visible) pointingError(blob!!.centerX, sampled.third).toFloat() else null,
            jitter, null, sampled.fourth?.brightness, when { windHold -> WindRisk.GUSTY; jitter == null -> WindRisk.UNKNOWN; else -> WindRisk.CALM }, nowMs())
        var action = "observe"

        val status = motor.status()
        lastStatusAtMs = nowMs()
        if (!status.connected) {
            action = "link-lost"
            linkState = LinkState.OFFLINE
            faults.add(TrackerFault.LINK_LOST)
            publish(position.azimuthDeg, position.elevationDeg)
            logCycle(position.azimuthDeg, position.elevationDeg, sampled, visible, action)
            return
        }
        linkState = LinkState.CONNECTED
        faults.remove(TrackerFault.LINK_LOST)
        if (status.busy) {
            action = "motor-busy"
            faults.add(TrackerFault.MOTOR_BUSY)
            publish(position.azimuthDeg, position.elevationDeg)
            logCycle(position.azimuthDeg, position.elevationDeg, sampled, visible, action)
            return
        }
        status.thetaDeg?.let { theta = it.coerceIn(-tuning.thetaLimitDeg, tuning.thetaLimitDeg) }
        if (faults.any { it == TrackerFault.JAM || it == TrackerFault.LIMIT_REACHED || it == TrackerFault.MOTOR_BUSY }) {
            action = "fault-hold"
            stopMotor()
            publish(position.azimuthDeg, position.elevationDeg)
            logCycle(position.azimuthDeg, position.elevationDeg, sampled, visible, action)
            return
        }
        if (!calibrated && !isNight) {
            action = "calibration-required"
            faults.add(TrackerFault.CALIBRATION_REQUIRED)
            stopMotor()
            publish(position.azimuthDeg, position.elevationDeg)
            logCycle(position.azimuthDeg, position.elevationDeg, sampled, visible, action)
            return
        }
        if (isNight) {
            action = "night-stow"
            cameraMisses = 0
            angleSource = AngleSource.DEAD_RECKONED
            faults.remove(TrackerFault.CAMERA_UNAVAILABLE)
        } else if (windHold || (visible && jitter != null && jitter > tuning.jitterThresholdPx)) {
            action = "wind-stow"
            cameraMisses = 0
            angleSource = AngleSource.DEAD_RECKONED
        } else if (visible) {
            action = "camera-track"
            cameraMisses = 0
            angleSource = AngleSource.CAMERA_LOCKED
            faults.remove(TrackerFault.CAMERA_UNAVAILABLE)
            // Camera observes the sun's horizontal image offset; the sun is the absolute reference.
            theta = (rawTarget - pointingError(blob!!.centerX, sampled.third)).coerceIn(-tuning.thetaLimitDeg, tuning.thetaLimitDeg)
            val rehomeDue = lastRehome?.let { Duration.between(it, instant) >= tuning.cameraRehomeInterval } ?: true
            if (rehomeDue && safe("camera-rehome-sync") { motor.sync(theta) } != null) {
                lastRehome = instant
                persist()
            }
            persist()
        } else {
            action = "dead-reckoned"
            angleSource = AngleSource.DEAD_RECKONED
            if (sampled.fourth == null) {
                cameraMisses++
                if (cameraMisses >= tuning.cameraRehomeAfterMisses) faults.add(TrackerFault.CAMERA_UNAVAILABLE)
            } else {
                cameraMisses = 0
                faults.remove(TrackerFault.CAMERA_UNAVAILABLE)
            }
        }
        if (isNight || windHold) {
            if (windHold) action = "wind-stow"
            moveToward(0.0, "safe-position")
            publish(position.azimuthDeg, position.elevationDeg)
            logCycle(position.azimuthDeg, position.elevationDeg, sampled, visible, action)
            return
        }
        if (abs(target - theta) > tuning.deadbandDeg) {
            action = "tracking-move"
            val stableSun = visible && (jitter == null || jitter <= tuning.jitterThresholdPx)
            moveToward(target, "move", blob.takeIf { stableSun }, sampled.third.takeIf { stableSun })
        }
        else if (action == "observe") action = "deadband-hold"
        publish(position.azimuthDeg, position.elevationDeg)
        logCycle(position.azimuthDeg, position.elevationDeg, sampled, visible, action)
    }

    private suspend fun sampleJitterAndSun(): Quad<Float?, SunBlob?, Int, SkyStats?> {
        val xs = mutableListOf<Float>()
        val blobs = mutableListOf<SunBlob>()
        var last: RgbFrame? = null
        repeat(tuning.jitterFrames) { i ->
            val frame = nextFrameSafe()
            if (frame != null) {
                last = frame
                SunDetector.findSun(frame)?.let { xs += it.centerX; blobs += it }
            }
            if (i < tuning.jitterFrames - 1) sleeper.sleep(tuning.jitterFrameSpacing)
        }
        val jitter = if (xs.size >= 3) {
            val mean = xs.average()
            kotlin.math.sqrt(xs.sumOf { (it - mean) * (it - mean).toDouble() } / xs.size).toFloat()
        } else null
        val blob = if (blobs.isNotEmpty()) SunBlob(
            blobs.map { it.centerX }.average().toFloat(),
            blobs.map { it.centerY }.average().toFloat(),
            blobs.last().areaPixels
        ) else null
        val frameWidth = last?.width ?: 1
        val stats = last?.let { SunDetector.skyStats(it) }
        return Quad(jitter, blob, frameWidth, stats)
    }

    private fun updateWind(jitter: Float?) {
        if (jitter != null && jitter > tuning.jitterThresholdPx) { gustCycles++; calmCycles = 0 }
        else if (jitter != null) { calmCycles++; gustCycles = 0 }
        if (!windHold && gustCycles >= tuning.windEngageCycles) windHold = true
        if (windHold && calmCycles >= tuning.windReleaseCycles) { windHold = false; gustCycles = 0 }
    }

    private suspend fun moveToward(goal: Double, intent: String = "move", initialSun: SunBlob? = null, initialWidth: Int? = null) {
        val delta = (goal - theta).coerceIn(-tuning.thetaLimitDeg - theta, tuning.thetaLimitDeg - theta)
        if (abs(delta) <= tuning.deadbandDeg) return
        val signedSeconds = delta / thetaPerSecond
        val dir = if (signedSeconds > 0) MoveDirection.FORWARD else MoveDirection.REVERSE
        val expectedMs = min(tuning.maxMove.toMillis().toDouble(), abs(signedSeconds) * 1_000.0).toLong().coerceAtLeast(1)
        var referenceSun = initialSun
        var referenceWidth = initialWidth
        for (attempt in 0 until tuning.jamRetries) {
            val before = theta
            val actualMs = try {
                (safe("$intent:${dir.name}") { motor.move(dir, expectedMs) } ?: return).coerceIn(0, expectedMs)
            } catch (_: MotorLimitReachedException) {
                faults.add(TrackerFault.LIMIT_REACHED)
                stopMotor()
                publish()
                return
            }
            val expectedDeg = dir.sign * thetaPerSecond * actualMs / 1_000.0
            val afterStatus = motor.status()
            lastStatusAtMs = nowMs()
            val measured = afterStatus.thetaDeg
            val afterFrame = if (referenceSun != null && referenceWidth != null) nextFrameSafe() else null
            val afterSun = afterFrame?.let { SunDetector.findSun(it) }
            val observed = when {
                measured != null -> measured - before
                afterSun != null && referenceSun != null -> -(pointingError(afterSun.centerX, afterFrame!!.width) - pointingError(referenceSun.centerX, referenceWidth!!))
                else -> expectedDeg
            }
            if (actualMs > 0 && abs(expectedDeg) > 0.1 && abs(observed) < abs(expectedDeg) * tuning.jamExpectedFraction) jamStreak++ else jamStreak = 0
            if (jamStreak >= 2) { faults.add(TrackerFault.JAM); stopMotor(); return }
            val responseRatio = if (expectedDeg == 0.0) 0.0 else observed / expectedDeg
            if (measured == null && afterSun != null && afterFrame != null) {
                val afterPosition = SolarMath.position(tuning.latitudeDeg, tuning.longitudeDeg, clock.instant())
                val afterRawTarget = SolarMath.trackerAngle(afterPosition.azimuthDeg, afterPosition.elevationDeg)
                theta = (afterRawTarget - pointingError(afterSun.centerX, afterFrame.width)).coerceIn(-tuning.thetaLimitDeg, tuning.thetaLimitDeg)
                referenceSun = afterSun
                referenceWidth = afterFrame.width
            } else if (measured != null) {
                theta = measured.coerceIn(-tuning.thetaLimitDeg, tuning.thetaLimitDeg)
            } else {
                theta = (theta + expectedDeg).coerceIn(-tuning.thetaLimitDeg, tuning.thetaLimitDeg)
            }
            if (responseRatio in 0.5..1.5) thetaPerSecond *= 0.8 + 0.2 * responseRatio
            motor.updateCalibrationSpeed(thetaPerSecond)
            if (actualMs == 0L) break
            if (abs(goal - theta) <= tuning.deadbandDeg || actualMs < expectedMs) break
        }
        persist()
        publish()
    }

    override suspend fun stop() {
        job?.cancel(); job = null
        stopMotor()
        publish()
    }

    override suspend fun jog(deltaDeg: Float) {
        require(deltaDeg.isFinite() && deltaDeg != 0f && abs(deltaDeg) <= 5f) {
            "Manual jog must be a non-zero step of at most 5 degrees."
        }
        ensureLoaded()
        job?.cancelAndJoin()
        job = null
        stopMotor()
        val status = runCatching { motor.status() }.getOrElse {
            linkState = LinkState.OFFLINE
            throw IllegalStateException("Tracker status is unavailable; jog was not sent.", it)
        }
        lastStatusAtMs = nowMs()
        linkState = if (status.connected) LinkState.CONNECTED else LinkState.OFFLINE
        if (!status.connected) throw IllegalStateException("Tracker is offline; jog was not sent.")
        status.thetaDeg?.let { theta = it.coerceIn(-tuning.thetaLimitDeg, tuning.thetaLimitDeg) }
        if (!calibrated) throw IllegalStateException("Calibrate the tracker before manual jog.")
        if (safe("manual-jog-preflight") { true } != true) {
            throw IllegalStateException("Safety interlocks blocked manual jog; no motion was sent.")
        }
        val goal = (theta + deltaDeg).coerceIn(-tuning.thetaLimitDeg, tuning.thetaLimitDeg)
        if (goal == theta) throw IllegalStateException("Travel limit reached; jog was not sent.")
        target = goal
        moveToward(goal, "manual-jog")
        publish()
    }

    override fun calibrate(): Flow<com.solarrobo.core.contracts.CalibrationStep> = flow {
        ensureLoaded()
        suspend fun fail(step: String, message: String) {
            calibrated = false
            faults.add(TrackerFault.CALIBRATION_REQUIRED)
            persist(); publish()
            emit(com.solarrobo.core.contracts.CalibrationStep(step, 1f, message, false))
        }
        emit(com.solarrobo.core.contracts.CalibrationStep("camera", 0f, "Keep the filtered sun visible in the camera"))
        if (!homeDeclared) { fail("home", "Confirm the panel is physically flat and set home before calibration"); return@flow }
        val frame1 = nextFrameSafe()
        val sun1 = frame1?.let { SunDetector.findSun(it) }
        if (frame1 == null || sun1 == null) { fail("camera", "No sun blob found; check filter and visibility"); return@flow }
        faults.remove(TrackerFault.CAMERA_UNAVAILABLE)
        val calibrationStatus = motor.status()
        lastStatusAtMs = nowMs()
        if (!calibrationStatus.connected) { linkState = LinkState.OFFLINE; fail("link", "ESP32 link is offline"); return@flow }

        emit(com.solarrobo.core.contracts.CalibrationStep("direction", 0.2f, "Checking motor direction"))
        val forwardMs = safe("calibrate-forward") { motor.move(MoveDirection.FORWARD, 1_000L) }
        val frame2 = nextFrameSafe()
        val sun2 = frame2?.let { SunDetector.findSun(it) }
        if (forwardMs == null || forwardMs < 500L || frame2 == null || sun2 == null) {
            fail("direction", "Forward test failed or the sun left the camera view"); return@flow
        }
        val backwardMs = safe("calibrate-reverse") { motor.move(MoveDirection.REVERSE, 1_000L) }
        val frame3 = nextFrameSafe()
        val sun3 = frame3?.let { SunDetector.findSun(it) }
        if (backwardMs == null || backwardMs < 500L || frame3 == null || sun3 == null) {
            fail("direction", "Return test failed or the sun left the camera view"); return@flow
        }
        val fwdGain = rawOffsetDeg(sun2.centerX, frame2.width) - rawOffsetDeg(sun1.centerX, frame1.width)
        val backGain = rawOffsetDeg(sun3.centerX, frame3.width) - rawOffsetDeg(sun2.centerX, frame2.width)
        val imageGain = (fwdGain - backGain) / 2.0
        if (abs(imageGain) < 0.2) { fail("direction", "Motor moved the image too little; check motor and camera mount"); return@flow }

        emit(com.solarrobo.core.contracts.CalibrationStep("sun-drift", 0.45f, "Observing sun drift for six minutes"))
        val timeA = clock.instant()
        val imageAFrame = nextFrameSafe()
        val imageA = imageAFrame?.let { SunDetector.findSun(it) }
        if (imageAFrame == null || imageA == null) { fail("sun-drift", "Sun not visible before drift observation"); return@flow }
        sleeper.sleep(tuning.calibrationPause)
        val timeB = clock.instant()
        val imageBFrame = nextFrameSafe()
        val imageB = imageBFrame?.let { SunDetector.findSun(it) }
        if (imageBFrame == null || imageB == null) { fail("sun-drift", "Sun not visible after drift observation"); return@flow }
        val imageDrift = rawOffsetDeg(imageB.centerX, imageBFrame.width) - rawOffsetDeg(imageA.centerX, imageAFrame.width)
        val sunA = SolarMath.position(tuning.latitudeDeg, tuning.longitudeDeg, timeA)
        val sunB = SolarMath.position(tuning.latitudeDeg, tuning.longitudeDeg, timeB)
        val sunDrift = SolarMath.trackerAngle(sunB.azimuthDeg, sunB.elevationDeg) - SolarMath.trackerAngle(sunA.azimuthDeg, sunA.elevationDeg)
        if (abs(imageDrift) < 0.3 || abs(sunDrift) < 0.3) { fail("sun-drift", "Sun drift too small to determine camera direction; retry later"); return@flow }
        signX = if (imageDrift * sunDrift > 0.0) 1 else -1
        thetaPerSecond = -signX * imageGain
        if (abs(thetaPerSecond) < 0.2) { fail("direction", "Motor speed estimate is too small"); return@flow }
        motor.updateCalibrationSpeed(thetaPerSecond)
        if (safe("set-limits") { motor.setLimit(-tuning.thetaLimitDeg, tuning.thetaLimitDeg) } == null) {
            fail("limits", "Could not apply calibrated firmware travel limits"); return@flow
        }
        val sunNow = SolarMath.position(tuning.latitudeDeg, tuning.longitudeDeg, clock.instant())
        val rawSunTarget = SolarMath.trackerAngle(sunNow.azimuthDeg, sunNow.elevationDeg)
        target = rawSunTarget.coerceIn(-tuning.thetaLimitDeg, tuning.thetaLimitDeg)
        theta = (rawSunTarget - pointingError(imageB.centerX, imageBFrame.width)).coerceIn(-tuning.thetaLimitDeg, tuning.thetaLimitDeg)
        lastRehome = clock.instant()
        if (safe("sync") { motor.sync(theta) } == null) { fail("sync", "Could not sync the estimated panel position"); return@flow }
        calibrated = true
        faults.remove(TrackerFault.CALIBRATION_REQUIRED)
        persist(); publish(sunNow.azimuthDeg, sunNow.elevationDeg)
        emit(com.solarrobo.core.contracts.CalibrationStep("complete", 1f, "Calibration complete", true))
    }

    override suspend fun setHome() {
        ensureLoaded()
        if (safe("home") { motor.home() } == true) {
            theta = 0.0
            calibrated = false
            homeDeclared = true
            faults.add(TrackerFault.CALIBRATION_REQUIRED)
            lastRehome = clock.instant()
            persist()
        }
        publish()
    }
    override suspend fun zeroCamera() { cameraMisses = 0; angleSource = AngleSource.CAMERA_LOCKED; faults.remove(TrackerFault.CAMERA_UNAVAILABLE); persist(); publish() }
    override suspend fun resetFault() { faults.remove(TrackerFault.JAM); faults.remove(TrackerFault.LIMIT_REACHED); faults.remove(TrackerFault.MOTOR_BUSY); faults.remove(TrackerFault.CAMERA_UNAVAILABLE); jamStreak = 0; persist(); publish() }

    private suspend fun <T> safe(operation: String, action: suspend () -> T): T? {
        val status = runCatching { motor.status() }.getOrNull() ?: run {
            linkState = LinkState.OFFLINE
            faults.add(TrackerFault.LINK_LOST)
            return null
        }
        lastStatusAtMs = nowMs()
        linkState = if (status.connected) LinkState.CONNECTED else LinkState.OFFLINE
        val context = SafetyContext(
            link = linkState,
            statusFresh = status.connected && nowMs() - lastStatusAtMs in 0..5_000,
            motorBusy = status.busy,
            calibrated = calibrated,
            faultActive = faults.any { it != TrackerFault.CALIBRATION_REQUIRED },
            windHold = windHold,
            calibrationInProgress = operation.startsWith("calibrate-")
        )
        return if (safety.authorize(operation, context)) action() else null
    }
    private suspend fun stopMotor() {
        val context = SafetyContext(
            link = linkState,
            statusFresh = lastStatusAtMs > 0 && nowMs() - lastStatusAtMs in 0..5_000,
            motorBusy = true,
            calibrated = calibrated,
            faultActive = faults.any { it != TrackerFault.CALIBRATION_REQUIRED },
            windHold = windHold
        )
        if (safety.authorize("stop", context)) runCatching { motor.stop() }
    }
    private fun rawOffsetDeg(x: Float, width: Int): Double =
        ((x - (width - 1) / 2f) / max(1, width).toFloat() * tuning.cameraHorizontalFovDeg)
    private fun pointingError(x: Float, width: Int): Double = signX * rawOffsetDeg(x, width) - cameraBiasDeg
    private suspend fun logCycle(azimuth: Double, elevation: Double, sampled: Quad<Float?, SunBlob?, Int, SkyStats?>, visible: Boolean, action: String) {
        val now = nowMs()
        runCatching { persist() }
        val sink = logSink ?: return
        try {
            val stats = sampled.fourth
            sink.append(TrackerLogRecord(
                t = now, az = azimuth, el = elevation, thSun = target,
                lum = stats?.brightness, blueFrac = stats?.blueFraction, whiteFrac = stats?.whiteFraction,
                sunFound = visible, cx = sampled.second?.centerX, area = sampled.second?.areaPixels,
                jitter = sampled.first, est = theta, tps = thetaPerSecond,
                windHold = windHold, jam = TrackerFault.JAM in faults, action = action
            ))
            if (lastPruneAtMs == 0L || now - lastPruneAtMs >= Duration.ofDays(1).toMillis()) {
                val retentionCutoff = now - Duration.ofDays(90).toMillis()
                val completeDayCutoff = retentionCutoff / Duration.ofDays(1).toMillis() * Duration.ofDays(1).toMillis()
                sink.rollupAndPruneBefore(completeDayCutoff)
                lastPruneAtMs = now
            }
        } catch (_: Exception) {
            // Local logging failure must never stall or disable the physical safety loop.
        }
    }
    private fun nowMs() = clock.millis()
    private suspend fun nextFrameSafe(): RgbFrame? = withTimeoutOrNull(2_000L) { frames.nextFrame() }
    private suspend fun ensureLoaded() {
        if (stateLoaded) return
        val saved = store.load() ?: PersistedTrackerState()
        theta = saved.thetaDeg.coerceIn(-tuning.thetaLimitDeg, tuning.thetaLimitDeg)
        thetaPerSecond = saved.thetaPerSecond.takeIf { abs(it) >= 0.2 } ?: 3.0
        motor.updateCalibrationSpeed(thetaPerSecond)
        calibrated = saved.calibrated
        signX = if (saved.signX < 0) -1 else 1
        cameraBiasDeg = saved.cameraBiasDeg
        homeDeclared = saved.homeDeclared
        windHold = saved.windHold
        restoredFaults = saved.faultCodes.mapNotNull { code -> runCatching { TrackerFault.valueOf(code) }.getOrNull() }.toSet() +
            if (saved.jammed) setOf(TrackerFault.JAM) else emptySet()
        target = saved.lastTargetDeg.coerceIn(-tuning.thetaLimitDeg, tuning.thetaLimitDeg)
        stateLoaded = true
        publish()
    }
    private suspend fun persist() = store.save(PersistedTrackerState(
        theta, thetaPerSecond, calibrated, target, signX, cameraBiasDeg, homeDeclared,
        windHold, TrackerFault.JAM in faults, faults.map { it.name }.toSet()
    ))
    private fun failLink() { faults.add(TrackerFault.LINK_LOST); publish() }
    private fun snapshot(azimuth: Double? = null, elevation: Double? = null) = TrackerSnapshot(
        link = linkState, angleSource = angleSource,
        pointingErrorDeg = if (sky.value.sunVisible) (target - theta).toFloat() else null,
        sunAzimuthDeg = azimuth?.toFloat(), sunElevationDeg = elevation?.toFloat(),
        estimatedPanelAngleDeg = theta.toFloat(), targetAngleDeg = target.toFloat(), faults = faults.toList(),
        calibrated = calibrated, timestamp = nowMs()
    )
    private fun publish(azimuth: Double? = null, elevation: Double? = null) { state.value = snapshot(azimuth, elevation) }
    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}

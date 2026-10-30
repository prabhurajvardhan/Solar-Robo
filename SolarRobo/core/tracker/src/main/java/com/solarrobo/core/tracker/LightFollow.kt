package com.solarrobo.core.tracker

import com.solarrobo.core.contracts.LightFollowPhase
import com.solarrobo.core.contracts.LightFollowSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.sign
import kotlin.math.tan
import kotlin.math.PI

/** Result from a simple brightness-threshold detector; this is an image observation, not a sun sensor. */
data class LightSpot(val centerX: Float, val centerY: Float, val areaPixels: Int)
data class LightScan(val spot: LightSpot?, val saturatedFraction: Float)

object LightFinder {
    fun scan(frame: RgbFrame, minimumPeak: Float = 200f, brightRatio: Float = 0.9f, maximumAreaFraction: Float = 0.2f): LightScan {
        require(minimumPeak > 0f && brightRatio in 0f..1f && maximumAreaFraction in 0f..1f)
        val pixels = frame.width * frame.height
        var peak = 0f
        for (i in 0 until pixels) {
            val lum = (frame.red(i) + frame.green(i) + frame.blue(i)) / 3f
            if (lum > peak) peak = lum
        }
        if (peak < minimumPeak) return LightScan(null, 0f)
        val threshold = peak * brightRatio
        var area = 0
        var weightedX = 0.0
        var weightedY = 0.0
        var totalWeight = 0.0
        for (y in 0 until frame.height) for (x in 0 until frame.width) {
            val i = y * frame.width + x
            val lum = (frame.red(i) + frame.green(i) + frame.blue(i)) / 3f
            if (lum >= threshold) {
                area++
                weightedX += x * lum
                weightedY += y * lum
                totalWeight += lum
            }
        }
        val fraction = area.toFloat() / pixels
        if (fraction > maximumAreaFraction || area == 0 || totalWeight == 0.0) return LightScan(null, fraction)
        return LightScan(
            LightSpot((weightedX / totalWeight).toFloat(), (weightedY / totalWeight).toFloat(), area),
            fraction
        )
    }

    fun findLight(frame: RgbFrame): LightSpot? = scan(frame).spot

    /** Pinhole-camera horizontal angle from image center, matching the supplied tracker specification. */
    fun pixelToAngle(centerX: Float, imageWidth: Int = 320, horizontalFovDeg: Double = 65.0): Double {
        require(imageWidth > 0 && horizontalFovDeg in 1.0..179.0)
        val focalLength = (imageWidth / 2.0) / tan(Math.toRadians(horizontalFovDeg) / 2.0)
        return Math.toDegrees(atan2((centerX - imageWidth / 2.0).toDouble(), focalLength))
    }
}

/** One-motor-at-a-time torch/light follower. The injected move callback must apply hardware safety gates. */
class LightFollowLoop(
    private val frames: FrameSource,
    private val move: suspend (direction: Int, requestedMs: Long) -> Long,
    private val stopMotor: suspend () -> Unit,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val initialSunElevationDeg: Double? = null,
    private val imageWidth: Int = 320,
    private val horizontalFovDeg: Double = 65.0
) {
    private val _state = MutableStateFlow(LightFollowSnapshot())
    val state: StateFlow<LightFollowSnapshot> = _state.asStateFlow()
    private var pxPerForwardSecond: Double? = null
    private var warningShown = false
    private var moveCount = 0

    /** Runs until cancelled, stopped by a safety failure, or [maxCycles] is supplied for deterministic tests. */
    suspend fun run(maxCycles: Int? = null) {
        require(maxCycles == null || maxCycles >= 0)
        warningShown = initialSunElevationDeg != null && initialSunElevationDeg > 8.0
        moveCount = 0
        _state.value = LightFollowSnapshot(phase = LightFollowPhase.SEARCHING, darkFilterWarning = warningShown)
        try {
            val first = findFirstLight() ?: fail("No bright light in view")
            _state.value = LightFollowSnapshot(phase = LightFollowPhase.LEARNING, lightX = first.centerX, lightY = first.centerY, areaPixels = first.areaPixels, darkFilterWarning = warningShown)
            val forwardMs = move(1, 400L)
            val middle = readSpot()
            val reverseMs = move(-1, 400L)
            val last = readSpot()
            if (forwardMs <= 0L || reverseMs <= 0L) {
                fail("Learning failed: the motor did not run or the light disappeared.")
            }
            val middleSpot = middle ?: fail("Learning failed: the light disappeared during the forward move.")
            val lastSpot = last ?: fail("Learning failed: the light disappeared during the reverse move.")
            val forwardSeconds = forwardMs / 1_000.0
            if (forwardSeconds <= 0.0) fail("Learning failed: no forward motor run was measured.")
            val pxs = (((middleSpot.centerX - first.centerX) - (lastSpot.centerX - middleSpot.centerX)) / 2.0) / forwardSeconds
            if (!pxs.isFinite() || abs(pxs) < 5.0) fail("Learning failed: light movement was below 5 px/s; check the mount and motor.")
            pxPerForwardSecond = pxs

            var previous = lastSpot
            var missingFrames = 0
            var lastLightAt = nowMillis()
            var cycles = 0
            while (maxCycles == null || cycles < maxCycles) {
                cycles++
                val frame = frames.nextFrame()
                if (frame == null) {
                    missingFrames++
                    publishMissing(previous, missingFrames)
                    if (expired(lastLightAt)) return stopForNoLight()
                    continue
                }
                val scan = LightFinder.scan(frame)
                if (scan.saturatedFraction > 0.15f && !warningShown) {
                    warningShown = true
                }
                val spot = scan.spot
                if (spot == null) {
                    missingFrames++
                    publishMissing(previous, missingFrames, scan.saturatedFraction)
                    if (expired(lastLightAt)) return stopForNoLight()
                    continue
                }
                missingFrames = 0
                lastLightAt = nowMillis()
                val errorPx = spot.centerX - imageWidth / 2f
                val errorDeg = LightFinder.pixelToAngle(spot.centerX, imageWidth, horizontalFovDeg)
                val pxsNow = pxPerForwardSecond ?: return fail("Light-follow speed calibration is unavailable.")
                if (abs(errorPx) <= 6f) {
                    _state.value = snapshot(LightFollowPhase.LOCKED, spot, errorPx, errorDeg)
                    previous = spot
                    continue
                }
                val seconds = -errorPx / pxsNow * 0.8
                val duration = (abs(seconds) * 1_000.0).toLong().coerceIn(120L, 1_500L)
                val direction = if (seconds > 0.0) 1 else -1
                _state.value = snapshot(LightFollowPhase.MOVING, spot, errorPx, errorDeg)
                val ranMs = move(direction, duration)
                if (ranMs <= 0L) {
                    _state.value = snapshot(LightFollowPhase.END_OF_TRAVEL, spot, errorPx, errorDeg, "Travel limit reached; no motor time was accepted.")
                    return
                }
                moveCount++
                val verified = readSpot()
                if (verified == null) {
                    missingFrames++
                    publishMissing(previous, missingFrames)
                    if (expired(lastLightAt)) return stopForNoLight()
                    continue
                }
                missingFrames = 0
                lastLightAt = nowMillis()
                val seen = (verified.centerX - spot.centerX) / (ranMs / 1_000.0) * direction
                if (abs(seen) > 0.4 * abs(pxsNow) && abs(seen) < 2.5 * abs(pxsNow) && sign(seen) == sign(pxsNow)) {
                    pxPerForwardSecond = 0.8 * pxsNow + 0.2 * seen
                }
                previous = verified
                val verifyErrorPx = verified.centerX - imageWidth / 2f
                _state.value = snapshot(
                    if (abs(verifyErrorPx) <= 6f) LightFollowPhase.LOCKED else LightFollowPhase.MOVING,
                    verified, verifyErrorPx,
                    LightFinder.pixelToAngle(verified.centerX, imageWidth, horizontalFovDeg)
                )
            }
        } catch (cancelled: CancellationException) {
            runCatching { stopMotor() }
            throw cancelled
        } catch (error: Throwable) {
            runCatching { stopMotor() }
            _state.value = _state.value.copy(
                phase = LightFollowPhase.ERROR,
                message = error.localizedMessage ?: "Light-follow stopped after a safety or camera error.",
                darkFilterWarning = warningShown
            )
        }
    }

    suspend fun stop() {
        runCatching { stopMotor() }
        _state.value = LightFollowSnapshot(phase = LightFollowPhase.IDLE)
    }

    private suspend fun findFirstLight(): LightSpot? {
        repeat(20) {
            val frame = frames.nextFrame() ?: return@repeat
            val scan = LightFinder.scan(frame)
            if (scan.saturatedFraction > 0.15f && !warningShown) {
                warningShown = true
                _state.value = _state.value.copy(darkFilterWarning = true)
            }
            scan.spot?.let { return it }
        }
        return null
    }

    private suspend fun readSpot(): LightSpot? {
        val frame = frames.nextFrame() ?: return null
        val scan = LightFinder.scan(frame)
        if (scan.saturatedFraction > 0.15f && !warningShown) {
            warningShown = true
            _state.value = _state.value.copy(darkFilterWarning = true)
        }
        return scan.spot
    }

    private fun publishMissing(last: LightSpot, count: Int, brightFraction: Float = 0f) {
        val warning = warningShown || (brightFraction > 0.15f)
        if (warning) warningShown = true
        _state.value = LightFollowSnapshot(
            phase = if (count >= 3) LightFollowPhase.LOST else LightFollowPhase.SEARCHING,
            lightX = last.centerX,
            lightY = last.centerY,
            areaPixels = last.areaPixels,
        moves = moveCount,
            pxPerForwardSecond = pxPerForwardSecond?.toFloat(),
            message = if (count >= 3) "Light lost; holding position and continuing to search." else null,
            darkFilterWarning = warning
        )
    }

    private fun snapshot(
        phase: LightFollowPhase,
        spot: LightSpot,
        errorPx: Float,
        errorDeg: Double,
        message: String? = null
    ) = LightFollowSnapshot(
        phase = phase,
        lightX = spot.centerX,
        lightY = spot.centerY,
        areaPixels = spot.areaPixels,
        errorPx = errorPx,
        errorDeg = errorDeg.toFloat(),
        moves = moveCount,
        pxPerForwardSecond = pxPerForwardSecond?.toFloat(),
        message = message,
        darkFilterWarning = warningShown
    )

    private fun expired(lastLightAt: Long): Boolean = nowMillis() - lastLightAt >= NO_LIGHT_TIMEOUT_MS

    private suspend fun stopForNoLight() {
        runCatching { stopMotor() }
        _state.value = _state.value.copy(phase = LightFollowPhase.ERROR, message = "No light for 10 minutes; follow stopped.")
    }

    private fun fail(message: String): Nothing {
        _state.value = _state.value.copy(phase = LightFollowPhase.ERROR, message = message, darkFilterWarning = warningShown)
        throw IllegalStateException(message)
    }

    companion object {
        const val NO_LIGHT_TIMEOUT_MS = 10 * 60 * 1_000L
    }
}

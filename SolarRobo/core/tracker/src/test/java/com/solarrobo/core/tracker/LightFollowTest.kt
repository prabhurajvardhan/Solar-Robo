package com.solarrobo.core.tracker

import com.solarrobo.core.contracts.LightFollowPhase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LightFollowTest {
    @Test fun staticTargetConvergesForBothImageAndMotorSigns() = runBlocking {
        for (imageSign in listOf(1, -1)) for (motorSpeed in listOf(2.4, -2.4)) {
            val world = FollowWorld(imageSign, motorSpeed, { 10.0 }, 5_000)
            val loop = world.loop()
            loop.run(maxCycles = 60)
            val error = abs(world.lightAtNow() - world.panel)
            assertTrue("sign=$imageSign motor=$motorSpeed error=$error phase=${loop.state.value.phase}", error < 1.5)
        }
    }

    @Test fun followsMovingTargetAtEightTenthsDegreePerSecond() = runBlocking {
        val world = FollowWorld(1, 2.4, { 0.0 }, 30_000)
        world.light = { 10.0 + 0.8 * world.time }
        val loop = world.loop()
        loop.run(maxCycles = 140)
        assertTrue("target error=${abs(world.lightAtNow() - world.panel)}", abs(world.lightAtNow() - world.panel) < 3.0)
    }

    @Test fun reacquiresAfterFourteenDegreeTargetJump() = runBlocking {
        val world = FollowWorld(-1, 2.4, { 8.0 }, 5_000)
        world.light = { if (world.time < 30.0) 8.0 else -6.0 }
        val loop = world.loop()
        loop.run(maxCycles = 180)
        assertTrue("target error=${abs(world.lightAtNow() - world.panel)}", abs(world.lightAtNow() - world.panel) < 1.5)
    }

    @Test fun motorRuntimeNeverExceedsConfiguredTravelLimit() = runBlocking {
        val world = FollowWorld(1, 2.4, { 20.0 }, 5_000)
        val loop = world.loop()
        loop.run(maxCycles = 80)
        assertTrue(abs(world.netMs) <= world.limitMs)
    }

    @Test fun dimDistractorDoesNotCaptureAndDetectorIsFast() {
        val frame = FollowWorld(1, 2.4, { 0.0 }, 5_000).frame(includeTorch = false)
        val spot = LightFinder.findLight(frame)
        assertTrue("dim distractor-only frame should not produce a spot", spot == null)
        val bright = FollowWorld(1, 2.4, { 0.0 }, 5_000).frame(includeTorch = true)
        repeat(10) { LightFinder.findLight(bright) }
        val start = System.nanoTime()
        repeat(200) { LightFinder.findLight(bright) }
        val milliseconds = (System.nanoTime() - start) / 200.0 / 1_000_000.0
        assertTrue("findLight took $milliseconds ms", milliseconds < 20.0)
    }

    @Test fun learnsThenLocksAndReportsWarningForSunElevationAboveEightDegrees() = runBlocking {
        val world = FollowWorld(1, 2.4, { 0.0 }, 5_000)
        val loop = world.loop(sunElevation = 9.0)
        loop.run(maxCycles = 1)
        assertEquals(true, loop.state.value.darkFilterWarning)
        assertTrue(loop.state.value.phase in setOf(LightFollowPhase.LOCKED, LightFollowPhase.MOVING))
    }

    private class FollowWorld(
        val imageSign: Int,
        val motorSpeed: Double,
        var light: (Double) -> Double,
        val limitMs: Long,
        val hfov: Double = 70.0
    ) {
        var time = 0.0
        var panel = 0.0
        var netMs = 0L
        fun lightAtNow() = light(time)

        fun frame(includeTorch: Boolean = true): RgbFrame {
            time += 0.5
            val pixels = ByteArray(320 * 240 * 3)
            for (i in 0 until 320 * 240) {
                pixels[i * 3] = 35
                pixels[i * 3 + 1] = 35
                pixels[i * 3 + 2] = 35
            }
            // 6x6 dim distractor at 190 is below the 90%-of-peak bright threshold.
            for (y in 40 until 46) for (x in 40 until 46) set(pixels, x, y, 190, 190, 185)
            if (includeTorch) {
                val center = (160 + imageSign * (light(time) - panel) * 320.0 / hfov).toInt()
                if (center in 4..315) for (y in 110 until 116) for (x in center - 3 until center + 3) set(pixels, x, y, 255, 255, 255)
            }
            return RgbFrame(320, 240, pixels)
        }

        fun loop(sunElevation: Double? = null): LightFollowLoop = LightFollowLoop(
            frames = FrameSource { frame() },
            move = { direction, requested ->
                val room = if (direction > 0) limitMs - netMs else limitMs + netMs
                val ran = requested.coerceAtMost(room.coerceAtLeast(0))
                netMs += direction * ran
                panel += direction * ran / 1_000.0 * motorSpeed
                time += ran / 1_000.0 + 0.4
                ran
            },
            stopMotor = {},
            nowMillis = { (time * 1_000).toLong() },
            initialSunElevationDeg = sunElevation,
            horizontalFovDeg = hfov
        )

        private fun set(pixels: ByteArray, x: Int, y: Int, r: Int, g: Int, b: Int) {
            val i = (y * 320 + x) * 3
            pixels[i] = r.toByte(); pixels[i + 1] = g.toByte(); pixels[i + 2] = b.toByte()
        }
    }
}

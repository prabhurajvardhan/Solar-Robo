package com.solarrobo.core.weather

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SkyOpticalFlowTest {
    @Test fun estimatesApparentShiftWithoutCallingItWind() {
        val width = 160; val height = 120; val shift = 4
        val previous = ByteArray(width * height * 3)
        Random(41).nextBytes(previous)
        val current = ByteArray(previous.size)
        for (y in 0 until height) for (x in 0 until width) for (c in 0..2) {
            val sourceX = x - shift
            current[(y * width + x) * 3 + c] = if (sourceX >= 0) previous[(y * width + sourceX) * 3 + c] else 0
        }
        val result = SkyOpticalFlow.estimate(previous, current, width, height, intervalSeconds = 2.0)
        assertTrue(result.matchedPatches >= 3)
        assertEquals(2.0, result.dxPixelsPerSecond!!, 0.75)
        assertNotNull(result.confidence)
    }

    @Test fun uniformFramesReturnUnavailableFlow() {
        val pixels = ByteArray(160 * 120 * 3) { 90.toByte() }
        val result = SkyOpticalFlow.estimate(pixels, pixels, 160, 120, intervalSeconds = 1.0)
        assertNull(result.dxPixelsPerSecond)
        assertNull(result.dyPixelsPerSecond)
    }

    private fun assertNotNull(value: Any?) { assertTrue(value != null) }
}

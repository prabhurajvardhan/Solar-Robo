package com.solarrobo.core.tracker

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SunMathAndDetectorTest {
    @Test fun matchesFortyPythonReferenceFixturesAcrossFourSeasons() {
        val fixtures = listOf(
            "2026-01-15T00:00:00Z" to Pair(107.93484967, -14.44794651), "2026-01-15T03:00:00Z" to Pair(124.25863246, 24.20388794),
            "2026-01-15T06:00:00Z" to Pair(167.02571594, 49.81442113), "2026-01-15T09:00:00Z" to Pair(223.60951452, 37.23638890),
            "2026-01-15T12:00:00Z" to Pair(247.23492178, 1.51032911), "2026-01-15T15:00:00Z" to Pair(257.25702713, -39.30363404),
            "2026-01-15T18:00:00Z" to Pair(249.52231619, -81.05456641), "2026-01-15T21:00:00Z" to Pair(100.91811159, -56.01191106),
            "2026-03-20T00:00:00Z" to Pair(87.57191179, -8.15137385), "2026-03-20T03:00:00Z" to Pair(103.17050703, 34.36432396),
            "2026-03-20T06:00:00Z" to Pair(154.53886237, 69.93626759), "2026-03-20T09:00:00Z" to Pair(247.11254304, 49.75315862),
            "2026-03-20T12:00:00Z" to Pair(267.31098942, 8.02713319), "2026-03-20T15:00:00Z" to Pair(282.97933652, -34.46863379),
            "2026-03-20T18:00:00Z" to Pair(334.53413616, -70.03043717), "2026-03-20T21:00:00Z" to Pair(67.12367488, -49.71483247),
            "2026-06-21T00:00:00Z" to Pair(65.60478369, 0.96471037), "2026-06-21T03:00:00Z" to Pair(74.34933606, 41.30204184),
            "2026-06-21T06:00:00Z" to Pair(49.81469543, 81.52743274), "2026-06-21T09:00:00Z" to Pair(285.19187618, 54.22413447),
            "2026-06-21T12:00:00Z" to Pair(290.64211633, 13.34466843), "2026-06-21T15:00:00Z" to Pair(307.40346424, -24.35995231),
            "2026-06-21T18:00:00Z" to Pair(350.30002129, -47.88541822), "2026-06-21T21:00:00Z" to Pair(43.07851316, -34.35628300),
            "2026-09-22T00:00:00Z" to Pair(88.12132563, -4.47127988), "2026-09-22T03:00:00Z" to Pair(104.30445675, 37.95636735),
            "2026-09-22T06:00:00Z" to Pair(164.61469920, 71.57077508), "2026-09-22T09:00:00Z" to Pair(250.12747616, 46.66065165),
            "2026-09-22T12:00:00Z" to Pair(268.69640372, 4.61297495), "2026-09-22T15:00:00Z" to Pair(284.89929997, -37.80405156),
            "2026-09-22T18:00:00Z" to Pair(345.05578184, -71.19977318), "2026-09-22T21:00:00Z" to Pair(69.77048543, -46.48749322),
            "2026-12-21T00:00:00Z" to Pair(110.87231278, -12.45623219), "2026-12-21T03:00:00Z" to Pair(127.97420872, 25.10588042),
            "2026-12-21T06:00:00Z" to Pair(171.62637681, 48.03314877), "2026-12-21T09:00:00Z" to Pair(223.83438312, 33.72059650),
            "2026-12-21T12:00:00Z" to Pair(245.87944505, -1.75281141), "2026-12-21T15:00:00Z" to Pair(254.42436326, -42.12673853),
            "2026-12-21T18:00:00Z" to Pair(226.13199173, -82.15798108), "2026-12-21T21:00:00Z" to Pair(105.16533897, -53.41363707)
        )
        assertEquals(40, fixtures.size)
        fixtures.forEach { (time, expected) ->
            val actual = SolarMath.position(18.1067, 83.3956, Instant.parse(time))
            assertEquals("azimuth at $time", expected.first, actual.azimuthDeg, 0.01)
            assertEquals("elevation at $time", expected.second, actual.elevationDeg, 0.01)
        }
    }

    @Test fun trackerAngleUsesEastPositiveConvention() {
        assertEquals(0.0, SolarMath.trackerAngle(0.0, 90.0), 1e-6)
        assertTrue(SolarMath.trackerAngle(90.0, 30.0) > 0.0)
        assertTrue(SolarMath.trackerAngle(270.0, 30.0) < 0.0)
    }

    @Test fun detectsOnlyValidBrightBlobs() {
        val bytes = ByteArray(10 * 10 * 3) { 100 }
        for (y in 4..5) for (x in 4..5) {
            val i = (y * 10 + x) * 3
            bytes[i] = 255.toByte(); bytes[i + 1] = 255.toByte(); bytes[i + 2] = 255.toByte()
        }
        assertNotNull(SunDetector.findSun(RgbFrame(10, 10, bytes)))
        bytes.fill(255.toByte())
        assertNull(SunDetector.findSun(RgbFrame(10, 10, bytes)))
    }

    @Test fun rejectsTooSmallBlob() {
        val bytes = ByteArray(5 * 5 * 3) { 0 }
        val i = (2 * 5 + 2) * 3
        bytes[i] = 255.toByte(); bytes[i + 1] = 255.toByte(); bytes[i + 2] = 255.toByte()
        assertNull(SunDetector.findSun(RgbFrame(5, 5, bytes)))
    }
}

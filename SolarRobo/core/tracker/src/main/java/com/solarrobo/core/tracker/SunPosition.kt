package com.solarrobo.core.tracker

import java.time.Instant
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** NOAA solar position. Azimuth is degrees clockwise from true north; elevation is degrees above horizon. */
data class SunPosition(val azimuthDeg: Double, val elevationDeg: Double)

object SolarMath {
    fun position(latitudeDeg: Double, longitudeDeg: Double, instant: Instant): SunPosition {
        require(latitudeDeg in -90.0..90.0) { "Latitude must be in [-90, 90]" }
        require(longitudeDeg in -180.0..180.0) { "Longitude must be in [-180, 180]" }
        val jd = instant.toEpochMilli() / 86_400_000.0 + 2_440_587.5
        val jc = (jd - 2_451_545.0) / 36_525.0
        val l0 = mod(280.46646 + jc * (36_000.76983 + jc * 0.0003032), 360.0)
        val m = 357.52911 + jc * (35_999.05029 - 0.0001537 * jc)
        val eccentricity = 0.016708634 - jc * (0.000042037 + 0.0000001267 * jc)
        val mr = Math.toRadians(m)
        val center = sin(mr) * (1.914602 - jc * (0.004817 + 0.000014 * jc)) +
            sin(2.0 * mr) * (0.019993 - 0.000101 * jc) + sin(3.0 * mr) * 0.000289
        val trueLongitude = l0 + center
        val omega = 125.04 - 1934.136 * jc
        val apparentLongitude = trueLongitude - 0.00569 - 0.00478 * sin(Math.toRadians(omega))
        val meanObliquity = 23.0 + (26.0 + (21.448 - jc * (46.815 + jc * (0.00059 - jc * 0.001813))) / 60.0) / 60.0
        val obliquity = meanObliquity + 0.00256 * cos(Math.toRadians(omega))
        val declination = kotlin.math.asin(sin(Math.toRadians(obliquity)) * sin(Math.toRadians(apparentLongitude)))
        val y = tan(Math.toRadians(obliquity) / 2.0).let { it * it }
        val l0r = Math.toRadians(l0)
        val equationOfTime = 4.0 * Math.toDegrees(
            y * sin(2.0 * l0r) - 2.0 * eccentricity * sin(mr) +
                4.0 * eccentricity * y * sin(mr) * cos(2.0 * l0r) -
                0.5 * y * y * sin(4.0 * l0r) - 1.25 * eccentricity * eccentricity * sin(2.0 * mr)
        )
        val utc = instant.atOffset(java.time.ZoneOffset.UTC)
        val minutes = utc.hour * 60.0 + utc.minute + utc.second / 60.0 + utc.nano / 60_000_000_000.0
        val trueSolarTime = mod(minutes + equationOfTime + 4.0 * longitudeDeg, 1440.0)
        val hourAngle = Math.toRadians(trueSolarTime / 4.0 - 180.0)
        val lat = Math.toRadians(latitudeDeg)
        val cosZenith = (sin(lat) * sin(declination) + cos(lat) * cos(declination) * cos(hourAngle)).coerceIn(-1.0, 1.0)
        val zenith = acos(cosZenith)
        val azimuth = mod(Math.toDegrees(kotlin.math.atan2(
            sin(hourAngle), cos(hourAngle) * sin(lat) - tan(declination) * cos(lat)
        )) + 180.0, 360.0)
        return SunPosition(azimuth, 90.0 - Math.toDegrees(zenith))
    }

    /** Ideal east-positive panel tilt for a horizontal north-south axis. */
    fun trackerAngle(azimuthDeg: Double, elevationDeg: Double): Double {
        val az = Math.toRadians(azimuthDeg)
        val el = Math.toRadians(elevationDeg)
        return Math.toDegrees(kotlin.math.atan2(cos(el) * sin(az), sin(el)))
    }

    private fun mod(value: Double, divisor: Double): Double = ((value % divisor) + divisor) % divisor
}

package com.solarrobo.core.model

/**
 * Astronomical and geometric sun position relative to panel location.
 */
data class SolarPosition(
    val azimuthDeg: Float,
    val elevationDeg: Float,
    val timestamp: Long
)

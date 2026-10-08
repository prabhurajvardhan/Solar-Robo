package com.solarrobo.core.model

/**
 * Mechanical boundary constraints for actuator tilt and rotation.
 */
data class PanelAngleLimits(
    val minAngleDeg: Float = -90f,
    val maxAngleDeg: Float = 90f,
    val safeStowAngleDeg: Float = 0f
) {
    fun clamp(angleDeg: Float): Float = angleDeg.coerceIn(minAngleDeg, maxAngleDeg)
    fun isWithinBounds(angleDeg: Float): Boolean = angleDeg in minAngleDeg..maxAngleDeg
}

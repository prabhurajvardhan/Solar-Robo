package com.solarrobo.core.common

/**
 * Deterministic time provider abstraction for testing and temporal calculations.
 */
fun interface TimeProvider {
    fun currentTimeMillis(): Long
}

class SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}

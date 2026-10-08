package com.solarrobo.core.model

/**
 * System and hardware revision metadata.
 */
data class SystemInfo(
    val firmwareVersion: String,
    val hardwareRevision: String,
    val buildTimestamp: Long
)

package com.solarrobo.core.contracts

enum class ActivityType {
    MOVEMENT,
    ENERGY,
    SAFETY,
    SYSTEM,
    AI,
    RECOVERY
}

data class ActivityEvent(
    val id: String,
    val type: ActivityType,
    val title: String,
    val detail: String,
    val timestamp: Long
)

package com.solarrobo.core.contracts

enum class SafetyLevel {
    NORMAL,
    CAUTION,
    PROTECTING,
    FAULT,
    EMERGENCY
}

data class SafetyEvent(
    val id: String,
    val level: SafetyLevel,
    val code: String,
    val message: String,
    val createdAt: Long,
    val acknowledged: Boolean = false
)

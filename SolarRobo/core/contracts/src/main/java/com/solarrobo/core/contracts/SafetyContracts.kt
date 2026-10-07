package com.solarrobo.core.contracts

/**
 * System safety criticality level.
 */
enum class SafetyLevel {
    NORMAL,
    CAUTION,
    PROTECTING,
    FAULT,
    EMERGENCY
}

/**
 * Safety audit and incident record.
 */
data class SafetyEvent(
    val id: String,
    val level: SafetyLevel,
    val code: String,
    val message: String,
    val createdAt: Long,
    val acknowledged: Boolean = false
)

/**
 * Deterministic decision emitted by the Safety Gate before hardware actuation.
 */
sealed interface SafetyDecision {
    data object Allow : SafetyDecision
    data class Block(val reason: String) : SafetyDecision
    data class Modify(val command: RoboCommand, val reason: String) : SafetyDecision
}

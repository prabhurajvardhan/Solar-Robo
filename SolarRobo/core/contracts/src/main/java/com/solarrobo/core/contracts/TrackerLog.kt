package com.solarrobo.core.contracts

import kotlinx.coroutines.flow.Flow

data class TrackerLogRecord(
    val t: Long,
    val az: Double,
    val el: Double,
    val thSun: Double,
    val lum: Float?,
    val blueFrac: Float?,
    val whiteFrac: Float?,
    val sunFound: Boolean,
    val cx: Float?,
    val area: Int?,
    val jitter: Float?,
    val est: Double,
    val tps: Double,
    val windHold: Boolean,
    val jam: Boolean,
    val action: String
)

interface TrackerLogSink {
    suspend fun append(record: TrackerLogRecord)
    suspend fun rollupAndPruneBefore(timestamp: Long)
    fun observeRecent(limit: Int = 500): Flow<List<TrackerLogRecord>>
    fun observeDailySummaries(): Flow<List<TrackerDailySummary>>
}

data class TrackerDailySummary(
    val dayStartMs: Long,
    val sampleCount: Int,
    val sunFoundCount: Int,
    val averageJitter: Float?,
    val maximumJitter: Float?,
    val windHoldCycles: Int,
    val jamCycles: Int,
    val minimumEstimatedAngle: Double,
    val maximumEstimatedAngle: Double,
    val averageEstimatedAngle: Double
)

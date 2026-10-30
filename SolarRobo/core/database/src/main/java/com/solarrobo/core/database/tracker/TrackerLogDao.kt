package com.solarrobo.core.database.tracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.solarrobo.core.contracts.TrackerDailySummary
import com.solarrobo.core.contracts.TrackerLogRecord
import com.solarrobo.core.contracts.TrackerLogSink
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
abstract class TrackerLogDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(entity: TrackerLogEntity)

    @Query("SELECT * FROM tracker_log ORDER BY t DESC LIMIT :limit")
    abstract fun observeRecentEntities(limit: Int): Flow<List<TrackerLogEntity>>

    @Query("SELECT * FROM tracker_daily_summary ORDER BY dayStartMs DESC")
    abstract fun observeDailySummaryEntities(): Flow<List<TrackerDailySummaryEntity>>

    @Query("""
        INSERT OR REPLACE INTO tracker_daily_summary
        (dayStartMs, sampleCount, sunFoundCount, averageJitter, maximumJitter, windHoldCycles, jamCycles,
         minimumEstimatedAngle, maximumEstimatedAngle, averageEstimatedAngle)
        SELECT (t / 86400000) * 86400000, COUNT(*),
               SUM(CASE WHEN sun_found = 1 THEN 1 ELSE 0 END), AVG(jitter), MAX(jitter),
               SUM(CASE WHEN wind_hold = 1 THEN 1 ELSE 0 END), SUM(CASE WHEN jam = 1 THEN 1 ELSE 0 END),
               MIN(est), MAX(est), AVG(est)
        FROM tracker_log WHERE t < :timestamp
        GROUP BY (t / 86400000) * 86400000
    """)
    abstract suspend fun rollupBefore(timestamp: Long)

    @Query("DELETE FROM tracker_log WHERE t < :timestamp")
    abstract suspend fun deleteBefore(timestamp: Long)

    @Transaction
    open suspend fun rollupAndPruneBefore(timestamp: Long) {
        rollupBefore(timestamp)
        deleteBefore(timestamp)
    }
}

@Singleton
class RoomTrackerLogSink @Inject constructor(
    private val database: TrackerLogDatabase
) : TrackerLogSink {
    override suspend fun append(record: TrackerLogRecord) {
        database.trackerLogDao().insert(record.toEntity())
    }

    override suspend fun rollupAndPruneBefore(timestamp: Long) {
        database.trackerLogDao().rollupAndPruneBefore(timestamp)
    }

    override fun observeRecent(limit: Int): Flow<List<TrackerLogRecord>> {
        require(limit > 0)
        return database.trackerLogDao().observeRecentEntities(limit).map { rows -> rows.map(TrackerLogEntity::toRecord) }
    }

    override fun observeDailySummaries(): Flow<List<TrackerDailySummary>> =
        database.trackerLogDao().observeDailySummaryEntities().map { rows -> rows.map(TrackerDailySummaryEntity::toContract) }
}

private fun TrackerLogRecord.toEntity() = TrackerLogEntity(
    t = t, az = az, el = el, thSun = thSun, lum = lum, blueFrac = blueFrac,
    whiteFrac = whiteFrac, sunFound = sunFound, cx = cx, area = area,
    jitter = jitter, est = est, tps = tps, windHold = windHold, jam = jam, action = action
)

private fun TrackerLogEntity.toRecord() = TrackerLogRecord(
    t = t, az = az, el = el, thSun = thSun, lum = lum, blueFrac = blueFrac,
    whiteFrac = whiteFrac, sunFound = sunFound, cx = cx, area = area,
    jitter = jitter, est = est, tps = tps, windHold = windHold, jam = jam, action = action
)

private fun TrackerDailySummaryEntity.toContract() = TrackerDailySummary(
    dayStartMs = dayStartMs, sampleCount = sampleCount, sunFoundCount = sunFoundCount,
    averageJitter = averageJitter, maximumJitter = maximumJitter,
    windHoldCycles = windHoldCycles, jamCycles = jamCycles,
    minimumEstimatedAngle = minimumEstimatedAngle, maximumEstimatedAngle = maximumEstimatedAngle,
    averageEstimatedAngle = averageEstimatedAngle
)

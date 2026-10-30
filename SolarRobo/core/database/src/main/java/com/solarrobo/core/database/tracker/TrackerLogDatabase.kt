package com.solarrobo.core.database.tracker

import androidx.room.Database
import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase

@Entity(tableName = "tracker_log")
data class TrackerLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val t: Long,
    val az: Double,
    val el: Double,
    @ColumnInfo(name = "th_sun") val thSun: Double,
    val lum: Float?,
    @ColumnInfo(name = "blue_frac") val blueFrac: Float?,
    @ColumnInfo(name = "white_frac") val whiteFrac: Float?,
    @ColumnInfo(name = "sun_found") val sunFound: Boolean,
    val cx: Float?,
    val area: Int?,
    val jitter: Float?,
    val est: Double,
    val tps: Double,
    @ColumnInfo(name = "wind_hold") val windHold: Boolean,
    val jam: Boolean,
    val action: String
)

@Entity(tableName = "tracker_daily_summary")
data class TrackerDailySummaryEntity(
    @PrimaryKey val dayStartMs: Long,
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

@Database(entities = [TrackerLogEntity::class, TrackerDailySummaryEntity::class], version = 1, exportSchema = false)
abstract class TrackerLogDatabase : RoomDatabase() {
    abstract fun trackerLogDao(): TrackerLogDao
}

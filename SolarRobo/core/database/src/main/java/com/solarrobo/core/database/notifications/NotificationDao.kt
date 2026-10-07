package com.solarrobo.core.database.notifications

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC, id DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM notifications
            WHERE title = :title
                AND priority = :priority
                AND timestamp BETWEEN :earliestTimestamp AND :latestTimestamp
        )
        """
    )
    suspend fun hasRecentDuplicate(
        title: String,
        priority: String,
        earliestTimestamp: Long,
        latestTimestamp: Long
    ): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(notification: NotificationEntity): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String): Int
}

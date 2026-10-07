package com.solarrobo.core.database.notifications

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.solarrobo.core.contracts.NotificationEvent
import com.solarrobo.core.contracts.NotificationPriority

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val priority: String,
    val timestamp: Long,
    val isRead: Boolean,
    val actionDeepLink: String?
) {
    fun toContract(): NotificationEvent = NotificationEvent(
        id = id,
        title = title,
        body = body,
        priority = NotificationPriority.valueOf(priority),
        timestamp = timestamp,
        isRead = isRead,
        actionDeepLink = actionDeepLink
    )

    companion object {
        fun fromContract(event: NotificationEvent): NotificationEntity = NotificationEntity(
            id = event.id,
            title = event.title,
            body = event.body,
            priority = event.priority.name,
            timestamp = event.timestamp,
            isRead = event.isRead,
            actionDeepLink = event.actionDeepLink
        )
    }
}

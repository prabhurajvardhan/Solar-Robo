package com.solarrobo.feature.notifications.domain

import com.solarrobo.core.contracts.NotificationEvent
import kotlinx.coroutines.flow.Flow

interface NotificationsRepository {
    fun getNotifications(): Flow<List<NotificationEvent>>
    fun getUnreadCount(): Flow<Int>
    fun getErrorMessage(): Flow<String?>
    suspend fun ingest(event: NotificationEvent)
    suspend fun markAsRead(id: String)
}

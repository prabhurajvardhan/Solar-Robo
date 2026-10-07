package com.solarrobo.feature.notifications.data

import android.database.sqlite.SQLiteException
import com.solarrobo.core.common.ApplicationScope
import com.solarrobo.core.common.NotificationEventBus
import com.solarrobo.core.contracts.NotificationEvent
import com.solarrobo.core.database.notifications.NotificationDao
import com.solarrobo.core.database.notifications.NotificationEntity
import com.solarrobo.feature.notifications.domain.NotificationsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationsRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao,
    eventBus: NotificationEventBus,
    @ApplicationScope applicationScope: CoroutineScope
) : NotificationsRepository {
    private val ingestMutex = Mutex()
    private val errorMessage = MutableStateFlow<String?>(null)

    init {
        applicationScope.launch {
            eventBus.events.collect { event ->
                ingest(event)
            }
        }
    }

    override fun getNotifications(): Flow<List<NotificationEvent>> = notificationDao.observeAll()
        .map { notifications -> notifications.map(NotificationEntity::toContract) }

    override fun getUnreadCount(): Flow<Int> = notificationDao.observeUnreadCount()

    override fun getErrorMessage(): Flow<String?> = errorMessage.asStateFlow()

    override suspend fun ingest(event: NotificationEvent) {
        try {
            ingestMutex.withLock {
                val earliestTimestamp = subtractSaturated(
                    event.timestamp,
                    DEDUPLICATION_WINDOW_MILLIS
                )
                val latestTimestamp = addSaturated(
                    event.timestamp,
                    DEDUPLICATION_WINDOW_MILLIS
                )
                val duplicate = notificationDao.hasRecentDuplicate(
                    title = event.title,
                    priority = event.priority.name,
                    earliestTimestamp = earliestTimestamp,
                    latestTimestamp = latestTimestamp
                )
                if (!duplicate) {
                    notificationDao.insert(NotificationEntity.fromContract(event))
                }
            }
            errorMessage.value = null
        } catch (error: SQLiteException) {
            errorMessage.value = error.message ?: "The notification could not be saved."
        }
    }

    override suspend fun markAsRead(id: String) {
        try {
            notificationDao.markAsRead(id)
            errorMessage.value = null
        } catch (error: SQLiteException) {
            errorMessage.value = error.message ?: "The notification could not be updated."
        }
    }

    private companion object {
        const val DEDUPLICATION_WINDOW_MILLIS = 5 * 60 * 1000L

        fun subtractSaturated(value: Long, amount: Long): Long =
            if (value < Long.MIN_VALUE + amount) Long.MIN_VALUE else value - amount

        fun addSaturated(value: Long, amount: Long): Long =
            if (value > Long.MAX_VALUE - amount) Long.MAX_VALUE else value + amount
    }
}

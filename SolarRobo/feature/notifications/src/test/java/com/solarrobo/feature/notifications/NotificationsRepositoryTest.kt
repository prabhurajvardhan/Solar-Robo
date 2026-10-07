package com.solarrobo.feature.notifications

import com.solarrobo.core.common.InProcessNotificationEventBus
import com.solarrobo.core.contracts.NotificationEvent
import com.solarrobo.core.contracts.NotificationPriority
import com.solarrobo.core.database.notifications.NotificationDao
import com.solarrobo.core.database.notifications.NotificationEntity
import com.solarrobo.feature.notifications.data.NotificationsRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsRepositoryTest {
    @Test
    fun eventBusInsertsNotificationsInNewestFirstOrder() = runTest {
        val database = FakeNotificationDao()
        val eventBus = InProcessNotificationEventBus()
        val repository = NotificationsRepositoryImpl(database, eventBus, backgroundScope)
        val older = event("older", "Tracker aligned", 1_700_000_000_000L)
        val newer = event("newer", "Battery warning", 1_700_000_060_000L)
        runCurrent()

        eventBus.publish(older)
        eventBus.publish(newer)
        runCurrent()

        assertEquals(listOf(newer.id, older.id), repository.getNotifications().first().map { it.id })
    }

    @Test
    fun ingestDeduplicatesMatchingRecentAlertsButKeepsLaterAlerts() = runTest {
        val database = FakeNotificationDao()
        val repository = NotificationsRepositoryImpl(
            database,
            InProcessNotificationEventBus(),
            backgroundScope
        )
        val first = event("first", "High wind", 1_700_000_000_000L)
        val duplicate = event("duplicate", "High wind", first.timestamp + 60_000L)
        val later = event("later", "High wind", first.timestamp + 600_000L)

        repository.ingest(first)
        repository.ingest(duplicate)
        repository.ingest(later)

        assertEquals(listOf(later.id, first.id), repository.getNotifications().first().map { it.id })
    }

    @Test
    fun markAsReadPersistsAndUpdatesUnreadCount() = runTest {
        val database = FakeNotificationDao()
        val repository = NotificationsRepositoryImpl(
            database,
            InProcessNotificationEventBus(),
            backgroundScope
        )
        val unread = event("unread", "Connection lost", 1_700_000_000_000L)
        repository.ingest(unread)

        assertEquals(1, repository.getUnreadCount().first())
        repository.markAsRead(unread.id)

        assertTrue(repository.getNotifications().first().single().isRead)
        assertEquals(0, repository.getUnreadCount().first())
        assertTrue(database.records.value.single().isRead)
    }

    @Test
    fun ingestPreservesReadStateWhenAnExistingIdIsReceivedAgain() = runTest {
        val database = FakeNotificationDao()
        val repository = NotificationsRepositoryImpl(
            database,
            InProcessNotificationEventBus(),
            backgroundScope
        )
        val original = event("same-id", "Recovered", 1_700_000_000_000L).copy(isRead = true)
        repository.ingest(original)
        repository.ingest(original.copy(isRead = false, timestamp = original.timestamp + 600_000L))

        assertEquals(1, repository.getNotifications().first().size)
        assertTrue(repository.getNotifications().first().single().isRead)
    }

    private fun event(id: String, title: String, timestamp: Long) = NotificationEvent(
        id = id,
        title = title,
        body = "Prototype scenario: $title.",
        priority = NotificationPriority.HIGH,
        timestamp = timestamp
    )
}

private class FakeNotificationDao : NotificationDao {
    val records = MutableStateFlow<List<NotificationEntity>>(emptyList())

    override fun observeAll(): Flow<List<NotificationEntity>> = records.map { items ->
        items.sortedWith(compareByDescending<NotificationEntity> { it.timestamp }.thenByDescending { it.id })
    }

    override fun observeUnreadCount(): Flow<Int> = records.map { items ->
        items.count { !it.isRead }
    }

    override suspend fun hasRecentDuplicate(
        title: String,
        priority: String,
        earliestTimestamp: Long,
        latestTimestamp: Long
    ): Boolean = records.value.any { item ->
        item.title == title &&
            item.priority == priority &&
            item.timestamp in earliestTimestamp..latestTimestamp
    }

    override suspend fun insert(notification: NotificationEntity): Long {
        if (records.value.any { it.id == notification.id }) return -1L
        records.value = records.value + notification
        return 1L
    }

    override suspend fun markAsRead(id: String): Int {
        val matching = records.value.filter { it.id == id && !it.isRead }
        if (matching.isEmpty()) return 0
        records.value = records.value.map { item ->
            if (item.id == id) item.copy(isRead = true) else item
        }
        return matching.size
    }
}

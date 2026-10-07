package com.solarrobo.feature.notifications.mock

import com.solarrobo.core.contracts.NotificationEvent
import com.solarrobo.core.contracts.NotificationPriority
import com.solarrobo.feature.notifications.domain.NotificationsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FakeNotificationsRepository(
    initialNotifications: List<NotificationEvent> = prototypeNotifications
) : NotificationsRepository {
    private val notifications = MutableStateFlow(initialNotifications.sortedByNewest())

    override fun getNotifications(): Flow<List<NotificationEvent>> = notifications.asStateFlow()

    override fun getUnreadCount(): Flow<Int> = notifications.map { items ->
        items.count { !it.isRead }
    }

    override fun getErrorMessage(): Flow<String?> = flowOf(null)

    override suspend fun ingest(event: NotificationEvent) {
        val earliestTimestamp = subtractSaturated(event.timestamp, DEDUPLICATION_WINDOW_MILLIS)
        val latestTimestamp = addSaturated(event.timestamp, DEDUPLICATION_WINDOW_MILLIS)
        val duplicate = notifications.value.any { current ->
            current.title == event.title &&
                current.priority == event.priority &&
                current.timestamp in earliestTimestamp..latestTimestamp
        }
        if (!duplicate && notifications.value.none { it.id == event.id }) {
            notifications.value = (notifications.value + event).sortedByNewest()
        }
    }

    override suspend fun markAsRead(id: String) {
        notifications.value = notifications.value.map { event ->
            if (event.id == id) event.copy(isRead = true) else event
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

private fun List<NotificationEvent>.sortedByNewest(): List<NotificationEvent> =
    sortedWith(compareByDescending<NotificationEvent> { it.timestamp }.thenByDescending { it.id })

private const val PROTOTYPE_BASE_TIMESTAMP = 1_700_000_000_000L

val prototypeNotifications = listOf(
    NotificationEvent(
        id = "DEMO-006",
        title = "Robo recovered",
        body = "Prototype scenario: a simulated connection was restored. No hardware event occurred.",
        priority = NotificationPriority.LOW,
        timestamp = PROTOTYPE_BASE_TIMESTAMP + 300_000L,
        isRead = false
    ),
    NotificationEvent(
        id = "DEMO-005",
        title = "Solar generation increased",
        body = "Prototype scenario: simulated solar generation increased.",
        priority = NotificationPriority.LOW,
        timestamp = PROTOTYPE_BASE_TIMESTAMP + 240_000L,
        isRead = true
    ),
    NotificationEvent(
        id = "DEMO-004",
        title = "Robo connection lost",
        body = "Prototype scenario: simulated device connection is unavailable.",
        priority = NotificationPriority.HIGH,
        timestamp = PROTOTYPE_BASE_TIMESTAMP + 180_000L,
        isRead = false
    ),
    NotificationEvent(
        id = "DEMO-003",
        title = "Battery reserve reached",
        body = "Prototype scenario: simulated battery reserve threshold was reached.",
        priority = NotificationPriority.HIGH,
        timestamp = PROTOTYPE_BASE_TIMESTAMP + 120_000L,
        isRead = false
    ),
    NotificationEvent(
        id = "DEMO-002",
        title = "Motor protection activated",
        body = "Prototype scenario: a simulated motor protection event was received.",
        priority = NotificationPriority.MEDIUM,
        timestamp = PROTOTYPE_BASE_TIMESTAMP + 60_000L,
        isRead = true
    ),
    NotificationEvent(
        id = "DEMO-001",
        title = "High wind detected",
        body = "Prototype scenario: simulated high wind requires attention.",
        priority = NotificationPriority.CRITICAL,
        timestamp = PROTOTYPE_BASE_TIMESTAMP,
        isRead = false
    )
)

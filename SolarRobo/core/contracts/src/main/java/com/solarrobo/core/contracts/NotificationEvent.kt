package com.solarrobo.core.contracts

enum class NotificationPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

data class NotificationEvent(
    val id: String,
    val title: String,
    val body: String,
    val priority: NotificationPriority,
    val timestamp: Long,
    val isRead: Boolean = false,
    val actionDeepLink: String? = null
)

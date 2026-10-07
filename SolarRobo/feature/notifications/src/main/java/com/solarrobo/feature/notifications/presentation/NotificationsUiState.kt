package com.solarrobo.feature.notifications.presentation

import com.solarrobo.core.contracts.NotificationEvent

data class NotificationsUiState(
    val notifications: List<NotificationEvent> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val isOffline: Boolean = true,
    val errorMessage: String? = null
)

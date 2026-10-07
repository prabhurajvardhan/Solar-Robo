package com.solarrobo.feature.notifications.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solarrobo.core.contracts.NotificationEvent
import com.solarrobo.core.contracts.NotificationPriority
import com.solarrobo.feature.notifications.components.NotificationItem
import com.solarrobo.feature.notifications.components.NotificationsCard
import com.solarrobo.feature.notifications.mock.prototypeNotifications

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    NotificationsScreenContent(
        state = state,
        onMarkRead = viewModel::markRead,
        onRetry = viewModel::retry,
        modifier = modifier
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreenContent(
    state: NotificationsUiState,
    onMarkRead: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Notifications (${state.unreadCount} unread)") })
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NotificationsCard(unreadCount = state.unreadCount)
            if (state.isOffline) {
                Text(
                    "Local inbox: notifications are stored on this device and available offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            state.errorMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
            when {
                state.isLoading -> LoadingState()
                state.notifications.isEmpty() && state.errorMessage == null -> EmptyState()
                else -> NotificationList(state.notifications, onMarkRead)
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(8.dp))
        Text("Loading notifications...")
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No notifications yet", style = MaterialTheme.typography.titleMedium)
        Text(
            "New system alerts will appear here.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun NotificationList(
    notifications: List<NotificationEvent>,
    onMarkRead: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(notifications, key = { notification -> notification.id }) { notification ->
            NotificationItem(
                item = notification,
                onClick = { onMarkRead(notification.id) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationsPopulatedPreview() {
    MaterialTheme {
        NotificationsScreenContent(
            state = NotificationsUiState(
                notifications = prototypeNotifications,
                unreadCount = prototypeNotifications.count { !it.isRead }
            ),
            onMarkRead = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationsEmptyPreview() {
    MaterialTheme {
        NotificationsScreenContent(
            state = NotificationsUiState(),
            onMarkRead = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationsLoadingPreview() {
    MaterialTheme {
        NotificationsScreenContent(
            state = NotificationsUiState(isLoading = true),
            onMarkRead = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationsErrorPreview() {
    MaterialTheme {
        NotificationsScreenContent(
            state = NotificationsUiState(errorMessage = "Notifications are unavailable."),
            onMarkRead = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationsOfflinePreview() {
    MaterialTheme {
        NotificationsScreenContent(
            state = NotificationsUiState(
                notifications = listOf(
                    NotificationEvent(
                        id = "PREVIEW-OFFLINE",
                        title = "Robo connection lost",
                        body = "Prototype scenario: simulated device connection is unavailable.",
                        priority = NotificationPriority.HIGH,
                        timestamp = 1_700_000_000_000L
                    )
                )
            ),
            onMarkRead = {},
            onRetry = {}
        )
    }
}

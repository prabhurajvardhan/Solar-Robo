package com.solarrobo.feature.notifications.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solarrobo.core.contracts.NotificationEvent
import com.solarrobo.core.contracts.NotificationPriority
import java.text.DateFormat
import java.util.Date

@Composable
fun NotificationItem(
    item: NotificationEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timestamp = remember(item.timestamp) {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(item.timestamp))
    }
    val priorityColors = when (item.priority) {
        NotificationPriority.CRITICAL -> MaterialTheme.colorScheme.errorContainer to
            MaterialTheme.colorScheme.onErrorContainer
        NotificationPriority.HIGH -> MaterialTheme.colorScheme.tertiaryContainer to
            MaterialTheme.colorScheme.onTertiaryContainer
        NotificationPriority.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer to
            MaterialTheme.colorScheme.onSecondaryContainer
        NotificationPriority.LOW -> MaterialTheme.colorScheme.primaryContainer to
            MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (item.isRead) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row {
                Surface(
                    color = priorityColors.first,
                    contentColor = priorityColors.second,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        item.priority.name.lowercase().replaceFirstChar(Char::uppercase),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (item.isRead) "Read" else "Unread",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (item.isRead) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Text(item.body, style = MaterialTheme.typography.bodyMedium)
            Text(
                timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!item.isRead) {
                Text(
                    "Tap to mark as read",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UnreadCriticalNotificationPreview() {
    MaterialTheme {
        NotificationItem(
            item = NotificationEvent(
                id = "PREVIEW-1",
                title = "High wind detected",
                body = "Prototype scenario: simulated high wind requires attention.",
                priority = NotificationPriority.CRITICAL,
                timestamp = 1_700_000_000_000L
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReadNotificationPreview() {
    MaterialTheme {
        NotificationItem(
            item = NotificationEvent(
                id = "PREVIEW-2",
                title = "Solar generation increased",
                body = "Prototype scenario: simulated solar generation increased.",
                priority = NotificationPriority.LOW,
                timestamp = 1_700_000_000_000L,
                isRead = true
            ),
            onClick = {}
        )
    }
}

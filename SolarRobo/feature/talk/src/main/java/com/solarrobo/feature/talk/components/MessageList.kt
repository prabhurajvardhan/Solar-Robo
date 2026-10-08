package com.solarrobo.feature.talk.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solarrobo.feature.talk.domain.ChatMessage
import com.solarrobo.feature.talk.domain.MessageSender

@Composable
fun MessageList(
    messages: List<ChatMessage>,
    isSending: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (messages.isEmpty()) {
            item {
                Text("Start a conversation with Robo.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        items(messages, key = { it.id }) { message ->
            val fromUser = message.sender == MessageSender.USER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start
            ) {
                Column {
                    Text(if (fromUser) "You" else "Robo", style = MaterialTheme.typography.labelMedium)
                    Surface(
                        color = if (fromUser) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(message.text, modifier = Modifier.padding(12.dp))
                    }
                    if (!fromUser && message.intent != null) {
                        Text("Intent: ${message.intent}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        if (isSending) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator()
                    Text("Robo is thinking...", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageListPreview() {
    MaterialTheme {
        MessageList(messages = emptyList(), isSending = false)
    }
}
package com.solarrobo.feature.talk.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun Composer(
    text: String,
    isSending: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("Type a message...") },
            singleLine = true,
            enabled = !isSending,
            modifier = Modifier.weight(1f)
        )
        Button(
            onClick = onSend,
            enabled = !isSending && text.isNotBlank()
        ) {
            Text(if (isSending) "Sending" else "Send")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ComposerPreview() {
    MaterialTheme {
        Composer(text = "Hello", isSending = false, onTextChange = {}, onSend = {})
    }
}
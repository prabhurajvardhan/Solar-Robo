package com.solarrobo.feature.talk.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solarrobo.feature.talk.components.Composer
import com.solarrobo.feature.talk.components.MessageList

@Composable
fun TalkScreen(viewModel: TalkViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TalkContent(state, viewModel::updateInput, viewModel::sendMessage, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TalkContent(
    state: TalkUiState,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text("Talk to Robo")
                    Text(
                        if (state.isSending) "Rule-based guidance · thinking…" else "Rule-based guidance · works offline",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            })
        },
        bottomBar = { Composer(text = state.inputText, isSending = state.isSending, onTextChange = onTextChange, onSend = onSend) },
        modifier = modifier
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            MessageList(messages = state.messages, isSending = state.isSending, modifier = Modifier.weight(1f))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TalkScreenPreview() {
    MaterialTheme { TalkContent(TalkUiState(), onTextChange = {}, onSend = {}) }
}

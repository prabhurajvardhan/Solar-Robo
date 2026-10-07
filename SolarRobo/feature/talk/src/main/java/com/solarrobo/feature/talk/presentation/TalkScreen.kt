package com.solarrobo.feature.talk.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solarrobo.feature.talk.components.Composer
import com.solarrobo.feature.talk.components.MessageList

@Composable
fun TalkScreen(
    viewModel: TalkViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    TalkContent(
        state = state,
        onTextChange = viewModel::updateInput,
        onSend = viewModel::sendMessage,
        modifier = modifier
    )
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
        topBar = { TopAppBar(title = { Text("Talk to Robo") }) },
        bottomBar = {
            Composer(
                text = state.inputText,
                isSending = state.isSending,
                onTextChange = onTextChange,
                onSend = onSend
            )
        },
        modifier = modifier
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            MessageList(
                messages = state.messages,
                isSending = state.isSending,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TalkScreenEmptyPreview() {
    MaterialTheme {
        TalkContent(state = TalkUiState(), onTextChange = {}, onSend = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun TalkScreenSendingPreview() {
    MaterialTheme {
        TalkContent(state = TalkUiState(isSending = true), onTextChange = {}, onSend = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun TalkScreenErrorPreview() {
    MaterialTheme {
        TalkContent(
            state = TalkUiState(error = "Unable to get a response right now."),
            onTextChange = {},
            onSend = {}
        )
    }
}
package com.solarrobo.feature.talk.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solarrobo.feature.talk.components.Composer
import com.solarrobo.feature.talk.components.MessageList
import com.solarrobo.feature.talk.R

@Composable
fun TalkScreen(
    viewModel: TalkViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TalkContent(
        state = state,
        onTextChange = viewModel::updateInput,
        onSend = viewModel::sendMessage,
        onModelSelected = viewModel::selectModel,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TalkContent(
    state: TalkUiState,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onModelSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            androidx.compose.foundation.Image(painterResource(R.drawable.robo_mascot), "Robo", Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                            Text("Talk to Robo", modifier = Modifier.padding(start = 8.dp))
                        }
                        Text(
                            if (state.isSending) "Robo is thinking…" else if (state.messages.lastOrNull()?.sender == com.solarrobo.feature.talk.domain.MessageSender.ROBO_AI) "Robo is speaking • offline" else "Robo is listening • offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ModelSelector(
                            models = state.installedModels,
                            selectedModelId = state.selectedModelId,
                            onModelSelected = onModelSelected
                        )
                    }
                }
            )
        },
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
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            if (state.messages.isEmpty() && state.error == null) {
                Spacer(Modifier.height(4.dp))
            }
            MessageList(
                messages = state.messages,
                isSending = state.isSending,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ModelSelector(
    models: List<com.solarrobo.core.ai.TernaryModel>,
    selectedModelId: String?,
    onModelSelected: (String) -> Unit
) {
    val expandedState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val expanded = expandedState.value
    val selected = models.firstOrNull { it.id == selectedModelId }
    androidx.compose.foundation.layout.Box {
        TextButton(onClick = { expandedState.value = true }, enabled = models.isNotEmpty()) {
            Text(selected?.name ?: if (models.isEmpty()) "Download a model in Models" else "Select model")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expandedState.value = false }) {
            models.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model.name) },
                    onClick = {
                        onModelSelected(model.id)
                        expandedState.value = false
                    }
                )
            }
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

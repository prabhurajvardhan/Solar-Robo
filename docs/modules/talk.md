# Module Specification: Robo Talk (`feature/talk`)

## 1. Exact Task & Responsibility
Natural language conversation interface for user queries about solar efficiency, tracker health, weather impacts, and system diagnostics. All AI inference is isolated strictly behind the `AiEngine` platform abstraction.

---

## 2. Exact Files & Directory Layout
```text
feature/talk/
├── build.gradle.kts
├── src/
│   ├── main/java/com/solarrobo/feature/talk/
│   │   ├── TalkModule.kt
│   │   ├── domain/
│   │   │   └── TalkRepository.kt
│   │   ├── data/
│   │   │   └── TalkRepositoryImpl.kt
│   │   ├── presentation/
│   │   │   ├── TalkViewModel.kt
│   │   │   ├── TalkUiState.kt
│   │   │   └── TalkScreen.kt
│   │   ├── components/
│   │   │   ├── TalkCard.kt
│   │   │   ├── MessageList.kt
│   │   │   ├── Composer.kt
│   │   │   └── VoiceState.kt
│   │   └── mock/
│   │       └── FakeTalkRepository.kt
│   └── test/java/com/solarrobo/feature/talk/
│       └── TalkViewModelTest.kt
```

---

## 3. Module I/O & Boundaries

| Dir | Resource | Source / Consumer | Meaning & Boundary Rule |
|:---:|:---------|:------------------|:------------------------|
| **IN** | Message / Transcript | User / Speech Input | User text or transcribed voice query. |
| **IN** | System Context | Repositories | Bounded snapshot context (solar W, battery, status). |
| **IN** | `AiEngine` | Core Platform Adapter | Model inference execution. |
| **OUT**| `ConversationMessage`| Room / UI State | Stored chat messages with timestamps and role. |
| **OUT**| AI Intent | Event Bus (Optional) | Detected intent (e.g. user asks to go to Energy screen). |

### Function-Level Tasks
- `buildRequest(m, c)`: `String + Map -> AiRequest`. Formulates prompt with concise bounded telemetry context.
- `sendMessage(m)`: `String -> Flow<ChatMessage>`. Adds user message to state, calls `AiEngine`, and appends response.
- `saveSummary(s)`: `Summary -> Result<Unit>`. Persists conversation summary to local Room DB.

### Execution Flow
```text
user message → build bounded request → AiEngine.generate() → format response → update chat state → optional navigation intent
```

### Must NOT Implement
- No direct physical actuation or actuator movement.
- No safety gate bypass or override.
- No direct Google GenAI / LLM SDK imports in UI composables.
- No unlimited raw history dumped into model context.

---

## 4. Complete Skeleton Code for Every File

### `domain/TalkRepository.kt`
```kotlin
package com.solarrobo.feature.talk.domain

import kotlinx.coroutines.flow.Flow

data class ChatMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val timestamp: Long
)

enum class MessageSender { USER, ROBO_AI }

interface TalkRepository {
    fun getMessageHistory(): Flow<List<ChatMessage>>
    suspend fun sendMessage(userText: String): ChatMessage
}
```

### `data/TalkRepositoryImpl.kt`
```kotlin
package com.solarrobo.feature.talk.data

import com.solarrobo.core.ai.AiEngine
import com.solarrobo.core.ai.AiRequest
import com.solarrobo.feature.talk.domain.ChatMessage
import com.solarrobo.feature.talk.domain.MessageSender
import com.solarrobo.feature.talk.domain.TalkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TalkRepositoryImpl @Inject constructor(
    private val aiEngine: AiEngine
) : TalkRepository {

    private val messages = MutableStateFlow<List<ChatMessage>>(emptyList())

    override fun getMessageHistory(): Flow<List<ChatMessage>> = messages.asStateFlow()

    override suspend fun sendMessage(userText: String): ChatMessage {
        val userMsg = ChatMessage(UUID.randomUUID().toString(), MessageSender.USER, userText, System.currentTimeMillis())
        messages.value = messages.value + userMsg

        val aiResponse = aiEngine.generate(AiRequest(userText, emptyMap()))
        val roboMsg = ChatMessage(UUID.randomUUID().toString(), MessageSender.ROBO_AI, aiResponse.text, System.currentTimeMillis())
        messages.value = messages.value + roboMsg
        return roboMsg
    }
}
```

### `TalkModule.kt`
```kotlin
package com.solarrobo.feature.talk

import com.solarrobo.feature.talk.data.TalkRepositoryImpl
import com.solarrobo.feature.talk.domain.TalkRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TalkModule {
    @Binds
    @Singleton
    abstract fun bindTalkRepository(impl: TalkRepositoryImpl): TalkRepository
}
```

### `presentation/TalkUiState.kt`
```kotlin
package com.solarrobo.feature.talk.presentation

import com.solarrobo.feature.talk.domain.ChatMessage

data class TalkUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isThinking: Boolean = false,
    val error: String? = null
)
```

### `presentation/TalkViewModel.kt`
```kotlin
package com.solarrobo.feature.talk.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.feature.talk.domain.TalkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TalkViewModel @Inject constructor(
    private val repository: TalkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TalkUiState())
    val uiState: StateFlow<TalkUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getMessageHistory().collect { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }
        }
    }

    fun send(messageText: String) {
        if (messageText.isBlank()) return
        _uiState.update { it.copy(isThinking = true) }
        viewModelScope.launch {
            try {
                repository.sendMessage(messageText)
                _uiState.update { it.copy(isThinking = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isThinking = false, error = e.localizedMessage) }
            }
        }
    }
}
```

### `presentation/TalkScreen.kt`
```kotlin
package com.solarrobo.feature.talk.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.feature.talk.components.Composer
import com.solarrobo.feature.talk.components.MessageList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalkScreen(
    viewModel: TalkViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Robo Talk Assistant") }) },
        bottomBar = {
            Composer(
                isThinking = state.isThinking,
                onSendMessage = { viewModel.send(it) }
            )
        },
        modifier = modifier
    ) { padding ->
        MessageList(
            messages = state.messages,
            isThinking = state.isThinking,
            modifier = Modifier.padding(padding).fillMaxSize()
        )
    }
}
```

### `components/MessageList.kt`
```kotlin
package com.solarrobo.feature.talk.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.feature.talk.domain.ChatMessage
import com.solarrobo.feature.talk.domain.MessageSender

@Composable
fun MessageList(
    messages: List<ChatMessage>,
    isThinking: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages) { msg ->
            val isUser = msg.sender == MessageSender.USER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(msg.text, modifier = Modifier.padding(12.dp))
                }
            }
        }
        if (isThinking) {
            item {
                Text("Robo is thinking...", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
```

### `components/Composer.kt`
```kotlin
package com.solarrobo.feature.talk.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Composer(
    isThinking: Boolean,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Ask about solar efficiency...") },
            modifier = Modifier.weight(1f)
        )
        Button(
            onClick = {
                onSendMessage(text)
                text = ""
            },
            enabled = !isThinking && text.isNotBlank()
        ) {
            Text("Send")
        }
    }
}
```

### `mock/FakeTalkRepository.kt`
```kotlin
package com.solarrobo.feature.talk.mock

import com.solarrobo.feature.talk.domain.ChatMessage
import com.solarrobo.feature.talk.domain.MessageSender
import com.solarrobo.feature.talk.domain.TalkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

class FakeTalkRepository : TalkRepository {
    override fun getMessageHistory(): Flow<List<ChatMessage>> = flowOf(
        listOf(
            ChatMessage("1", MessageSender.USER, "What is current efficiency?", 1000L),
            ChatMessage("2", MessageSender.ROBO_AI, "Solar generation is at 310 Watts, optimal for 11:30 AM sun elevation.", 2000L)
        )
    )

    override suspend fun sendMessage(userText: String): ChatMessage {
        return ChatMessage(UUID.randomUUID().toString(), MessageSender.ROBO_AI, "Echo: $userText", System.currentTimeMillis())
    }
}
```

### `src/test/java/com/solarrobo/feature/talk/TalkViewModelTest.kt`
```kotlin
package com.solarrobo.feature.talk

import com.solarrobo.feature.talk.mock.FakeTalkRepository
import com.solarrobo.feature.talk.presentation.TalkViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TalkViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: TalkViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = TalkViewModel(FakeTalkRepository())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun send_addsMessage() = runTest {
        viewModel.send("Hello Robo")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.isThinking)
    }
}
```

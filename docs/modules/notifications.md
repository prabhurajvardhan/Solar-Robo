# Module Specification: Notifications (`feature/notifications`)

## 1. Exact Task & Responsibility
Consume standardized notification events from the Event Bus, deduplicate repeated alarms, persist read/unread status, and display an inbox with unread badge counter.

---

## 2. Exact Files & Directory Layout
```text
feature/notifications/
├── build.gradle.kts
├── src/
│   ├── main/java/com/solarrobo/feature/notifications/
│   │   ├── NotificationsModule.kt
│   │   ├── domain/
│   │   │   └── NotificationsRepository.kt
│   │   ├── data/
│   │   │   └── NotificationsRepositoryImpl.kt
│   │   ├── presentation/
│   │   │   ├── NotificationsViewModel.kt
│   │   │   ├── NotificationsUiState.kt
│   │   │   └── NotificationsScreen.kt
│   │   ├── components/
│   │   │   ├── NotificationsCard.kt
│   │   │   └── NotificationItem.kt
│   │   └── mock/
│   │       └── FakeNotificationsRepository.kt
│   └── test/java/com/solarrobo/feature/notifications/
│       └── NotificationsViewModelTest.kt
```

---

## 3. Module I/O & Boundaries

| Dir | Resource | Source / Consumer | Meaning & Boundary Rule |
|:---:|:---------|:------------------|:------------------------|
| **IN** | `NotificationEvent`| Event Bus (`:core:common`) | Ingested system or safety alert notification. |
| **IN** | Read Action | User Interaction (UI) | Notification item tapped/dismissed by user. |
| **OUT**| `Notification[]` | Jetpack Compose UI | In-memory and persisted inbox list. |
| **OUT**| Read State | Room DB / StateFlow | Persisted read flag and updated unread count badge. |

### Function-Level Tasks
- `ingest(e)`: `NotificationEvent -> Unit`. Ingests event, performs deduplication to avoid alert fatigue, and stores in Room.
- `markRead(id)`: `String -> Unit`. Updates `isRead = true` for the specified notification ID.
- `unreadCount()`: `none -> Flow<Int>`. Streams real-time count of unread notifications for badge indicators.

### Execution Flow
```text
event arrives → deduplicate → persist to Room → update unread count → user taps → mark read
```

### Must NOT Implement
- No root-cause analysis logic (diagnostics belongs to `:feature:health`).
- No notification spamming (do not repeatedly chime for the same ongoing condition).
- No safety policy decisions.

---

## 4. Complete Skeleton Code for Every File

### `domain/NotificationsRepository.kt`
```kotlin
package com.solarrobo.feature.notifications.domain

import com.solarrobo.core.contracts.NotificationEvent
import kotlinx.coroutines.flow.Flow

interface NotificationsRepository {
    fun getNotifications(): Flow<List<NotificationEvent>>
    fun getUnreadCount(): Flow<Int>
    suspend fun ingest(event: NotificationEvent)
    suspend fun markAsRead(id: String)
}
```

### `data/NotificationsRepositoryImpl.kt`
```kotlin
package com.solarrobo.feature.notifications.data

import com.solarrobo.core.contracts.NotificationEvent
import com.solarrobo.core.contracts.NotificationPriority
import com.solarrobo.feature.notifications.domain.NotificationsRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationsRepositoryImpl @Inject constructor() : NotificationsRepository {
    private val inbox = MutableStateFlow<List<NotificationEvent>>(
        listOf(
            NotificationEvent("N1", "Tracker Realigned", "Panel adjusted +12° toward peak irradiance", NotificationPriority.LOW, System.currentTimeMillis() - 7200000L, true),
            NotificationEvent("N2", "High Wind Alert", "Safe stow activated due to 16 m/s wind gusts", NotificationPriority.HIGH, System.currentTimeMillis() - 3600000L, false)
        )
    )

    override fun getNotifications(): Flow<List<NotificationEvent>> = inbox.asStateFlow()

    override fun getUnreadCount(): Flow<Int> = inbox.map { list -> list.count { !it.isRead } }

    override suspend fun ingest(event: NotificationEvent) {
        // Deduplicate recent notifications with same title
        val existing = inbox.value.firstOrNull { it.title == event.title && System.currentTimeMillis() - it.timestamp < 300000L }
        if (existing == null) {
            inbox.value = listOf(event) + inbox.value
        }
    }

    override suspend fun markAsRead(id: String) {
        inbox.value = inbox.value.map { if (it.id == id) it.copy(isRead = true) else it }
    }
}
```

### `NotificationsModule.kt`
```kotlin
package com.solarrobo.feature.notifications

import com.solarrobo.feature.notifications.data.NotificationsRepositoryImpl
import com.solarrobo.feature.notifications.domain.NotificationsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationsModule {
    @Binds
    @Singleton
    abstract fun bindNotificationsRepository(impl: NotificationsRepositoryImpl): NotificationsRepository
}
```

### `presentation/NotificationsUiState.kt`
```kotlin
package com.solarrobo.feature.notifications.presentation

import com.solarrobo.core.contracts.NotificationEvent

data class NotificationsUiState(
    val notifications: List<NotificationEvent> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false
)
```

### `presentation/NotificationsViewModel.kt`
```kotlin
package com.solarrobo.feature.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.feature.notifications.domain.NotificationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationsRepository
) : ViewModel() {

    val uiState: StateFlow<NotificationsUiState> = combine(
        repository.getNotifications(),
        repository.getUnreadCount()
    ) { list, unread ->
        NotificationsUiState(notifications = list, unreadCount = unread, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotificationsUiState(isLoading = true)
    )

    fun markRead(id: String) {
        viewModelScope.launch {
            repository.markAsRead(id)
        }
    }
}
```

### `presentation/NotificationsScreen.kt`
```kotlin
package com.solarrobo.feature.notifications.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.feature.notifications.components.NotificationItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Notifications (${state.unreadCount} unread)") }) },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.notifications) { item ->
                NotificationItem(
                    item = item,
                    onClick = { viewModel.markRead(item.id) }
                )
            }
        }
    }
}
```

### `components/NotificationItem.kt`
```kotlin
package com.solarrobo.feature.notifications.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.core.contracts.NotificationEvent

@Composable
fun NotificationItem(
    item: NotificationEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (!item.isRead) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        modifier = modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(item.body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
```

### `mock/FakeNotificationsRepository.kt`
```kotlin
package com.solarrobo.feature.notifications.mock

import com.solarrobo.core.contracts.NotificationEvent
import com.solarrobo.feature.notifications.domain.NotificationsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeNotificationsRepository : NotificationsRepository {
    override fun getNotifications(): Flow<List<NotificationEvent>> = flowOf(emptyList())
    override fun getUnreadCount(): Flow<Int> = flowOf(0)
    override suspend fun ingest(event: NotificationEvent) {}
    override suspend fun markAsRead(id: String) {}
}
```

### `src/test/java/com/solarrobo/feature/notifications/NotificationsViewModelTest.kt`
```kotlin
package com.solarrobo.feature.notifications

import com.solarrobo.feature.notifications.mock.FakeNotificationsRepository
import com.solarrobo.feature.notifications.presentation.NotificationsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: NotificationsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = NotificationsViewModel(FakeNotificationsRepository())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialUnreadCount() = runTest {
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.unreadCount)
    }
}
```

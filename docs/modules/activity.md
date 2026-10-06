# Module Specification: Activity & Robo Memory (`feature/activity`)

## 1. Exact Task & Responsibility
Persist and display chronological ledger of significant system events, mode switches, tracking optimizations, safety triggers, and automated recoveries. Acts as the robot's event memory without logging high-frequency raw telemetry samples.

---

## 2. Exact Files & Directory Layout
```text
feature/activity/
├── build.gradle.kts
├── src/
│   ├── main/java/com/solarrobo/feature/activity/
│   │   ├── ActivityModule.kt
│   │   ├── domain/
│   │   │   └── ActivityRepository.kt
│   │   ├── data/
│   │   │   └── ActivityRepositoryImpl.kt
│   │   ├── presentation/
│   │   │   ├── ActivityViewModel.kt
│   │   │   ├── ActivityUiState.kt
│   │   │   └── ActivityScreen.kt
│   │   ├── components/
│   │   │   ├── ActivityCard.kt
│   │   │   └── ActivityItem.kt
│   │   └── mock/
│   │       └── FakeActivityRepository.kt
│   └── test/java/com/solarrobo/feature/activity/
│       └── ActivityViewModelTest.kt
```

---

## 3. Module I/O & Boundaries

| Dir | Resource | Source / Consumer | Meaning & Boundary Rule |
|:---:|:---------|:------------------|:------------------------|
| **IN** | `ActivityEvent` | Event Bus (`:core:common`) | Ingested discrete lifecycle or safety event. |
| **IN** | `ActivityFilter` | User Interaction | Filter by type (Movement, Energy, Safety, AI). |
| **OUT**| `ActivityEvent[]` | Jetpack Compose UI | Chronological timeline list sorted newest-first. |

### Function-Level Tasks
- `append(e)`: `ActivityEvent -> Result<Unit>`. Persists normalized discrete event to Room DB.
- `query(f)`: `ActivityFilter -> Flow<List<ActivityEvent>>`. Queries stored events matching filter, ordered by timestamp descending.
- `summarize(e)`: `ActivityEvent -> String`. Formats event into a concise user-facing timeline summary.

### Execution Flow
```text
event received → deduplicate/normalize → persist in Room DB → query filtered flow → render timeline
```

### Must NOT Implement
- No invented or synthetic events.
- No high-frequency raw sensor sampling (do NOT log 1Hz telemetry).
- No ownership of statistical analytics (analytics belongs to `:feature:analytics`).

---

## 4. Complete Skeleton Code for Every File

### `domain/ActivityRepository.kt`
```kotlin
package com.solarrobo.feature.activity.domain

import com.solarrobo.core.contracts.ActivityEvent
import com.solarrobo.core.contracts.ActivityType
import kotlinx.coroutines.flow.Flow

data class ActivityFilter(
    val selectedType: ActivityType? = null
)

interface ActivityRepository {
    fun observeEvents(filter: ActivityFilter): Flow<List<ActivityEvent>>
    suspend fun recordEvent(event: ActivityEvent)
}
```

### `data/ActivityRepositoryImpl.kt`
```kotlin
package com.solarrobo.feature.activity.data

import com.solarrobo.core.contracts.ActivityEvent
import com.solarrobo.core.contracts.ActivityType
import com.solarrobo.feature.activity.domain.ActivityFilter
import com.solarrobo.feature.activity.domain.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepositoryImpl @Inject constructor() : ActivityRepository {
    private val memoryEvents = MutableStateFlow<List<ActivityEvent>>(
        listOf(
            ActivityEvent("1", ActivityType.MOVEMENT, "Sun Tracking Started", "Tracking solar elevation at 42°", System.currentTimeMillis() - 3600000L),
            ActivityEvent("2", ActivityType.ENERGY, "Peak Generation Hit", "Generated 340W at 12:00 PM", System.currentTimeMillis() - 1800000L)
        )
    )

    override fun observeEvents(filter: ActivityFilter): Flow<List<ActivityEvent>> {
        return memoryEvents.map { list ->
            if (filter.selectedType == null) list
            else list.filter { it.type == filter.selectedType }
        }
    }

    override suspend fun recordEvent(event: ActivityEvent) {
        memoryEvents.value = listOf(event) + memoryEvents.value
    }
}
```

### `ActivityModule.kt`
```kotlin
package com.solarrobo.feature.activity

import com.solarrobo.feature.activity.data.ActivityRepositoryImpl
import com.solarrobo.feature.activity.domain.ActivityRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ActivityModule {
    @Binds
    @Singleton
    abstract fun bindActivityRepository(impl: ActivityRepositoryImpl): ActivityRepository
}
```

### `presentation/ActivityUiState.kt`
```kotlin
package com.solarrobo.feature.activity.presentation

import com.solarrobo.core.contracts.ActivityEvent
import com.solarrobo.core.contracts.ActivityType

data class ActivityUiState(
    val events: List<ActivityEvent> = emptyList(),
    val filterType: ActivityType? = null,
    val isLoading: Boolean = false
)
```

### `presentation/ActivityViewModel.kt`
```kotlin
package com.solarrobo.feature.activity.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.core.contracts.ActivityType
import com.solarrobo.feature.activity.domain.ActivityFilter
import com.solarrobo.feature.activity.domain.ActivityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val repository: ActivityRepository
) : ViewModel() {

    private val filterState = MutableStateFlow<ActivityType?>(null)

    val uiState: StateFlow<ActivityUiState> = filterState.flatMapLatest { type ->
        repository.observeEvents(ActivityFilter(type)).map { list ->
            ActivityUiState(events = list, filterType = type, isLoading = false)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ActivityUiState(isLoading = true)
    )

    fun setFilter(type: ActivityType?) {
        filterState.value = type
    }
}
```

### `presentation/ActivityScreen.kt`
```kotlin
package com.solarrobo.feature.activity.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.core.contracts.ActivityType
import com.solarrobo.feature.activity.components.ActivityItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Activity & Robo Memory") }) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.filterType == null,
                    onClick = { viewModel.setFilter(null) },
                    label = { Text("All") }
                )
                ActivityType.entries.take(3).forEach { type ->
                    FilterChip(
                        selected = state.filterType == type,
                        onClick = { viewModel.setFilter(type) },
                        label = { Text(type.name) }
                    )
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.events) { event ->
                    ActivityItem(event = event)
                }
            }
        }
    }
}
```

### `components/ActivityItem.kt`
```kotlin
package com.solarrobo.feature.activity.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.core.contracts.ActivityEvent

@Composable
fun ActivityItem(event: ActivityEvent, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(event.title, style = MaterialTheme.typography.titleMedium)
                Text(event.type.name, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(4.dp))
            Text(event.detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
```

### `mock/FakeActivityRepository.kt`
```kotlin
package com.solarrobo.feature.activity.mock

import com.solarrobo.core.contracts.ActivityEvent
import com.solarrobo.core.contracts.ActivityType
import com.solarrobo.feature.activity.domain.ActivityFilter
import com.solarrobo.feature.activity.domain.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeActivityRepository : ActivityRepository {
    override fun observeEvents(filter: ActivityFilter): Flow<List<ActivityEvent>> = flowOf(
        listOf(
            ActivityEvent("M1", ActivityType.MOVEMENT, "Safe Position Stowed", "Operator triggered safe stow.", 1000L),
            ActivityEvent("M2", ActivityType.SAFETY, "High Wind Warning", "Wind speed hit 16 m/s.", 2000L)
        )
    )

    override suspend fun recordEvent(event: ActivityEvent) {}
}
```

### `src/test/java/com/solarrobo/feature/activity/ActivityViewModelTest.kt`
```kotlin
package com.solarrobo.feature.activity

import com.solarrobo.feature.activity.mock.FakeActivityRepository
import com.solarrobo.feature.activity.presentation.ActivityViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ActivityViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = ActivityViewModel(FakeActivityRepository())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsEvents() = runTest {
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.events.size)
    }
}
```

# Module Specification: Health & Diagnostics (`feature/health`)

## 1. Exact Task & Responsibility
Present standardized subsystem diagnostic health checks for controller board, stepper/servo motors, battery cells, solar sensors, CameraX hardware, and network interfaces.

---

## 2. Exact Files & Directory Layout
```text
feature/health/
├── build.gradle.kts
├── src/
│   ├── main/java/com/solarrobo/feature/health/
│   │   ├── HealthModule.kt
│   │   ├── domain/
│   │   │   └── HealthRepository.kt
│   │   ├── data/
│   │   │   └── HealthRepositoryImpl.kt
│   │   ├── presentation/
│   │   │   ├── HealthViewModel.kt
│   │   │   ├── HealthUiState.kt
│   │   │   └── HealthScreen.kt
│   │   ├── components/
│   │   │   ├── HealthCard.kt
│   │   │   └── HealthCheckItem.kt
│   │   └── mock/
│   │       └── FakeHealthRepository.kt
│   └── test/java/com/solarrobo/feature/health/
│       └── HealthViewModelTest.kt
```

---

## 3. Module I/O & Boundaries

| Dir | Resource | Source / Consumer | Meaning & Boundary Rule |
|:---:|:---------|:------------------|:------------------------|
| **IN** | `DeviceHealth` | Device Adapter (`:core:device`) | Motor, battery, sensors, camera, controller status. |
| **OUT**| `HealthUiState` | Jetpack Compose UI | Subsystem status checklist and warnings. |
| **OUT**| `HealthEvent` | Event Bus | Broadcasts critical component degradation. |

### Function-Level Tasks
- `loadHealth()`: `none -> Flow<DeviceHealth>`. Reads real-time hardware diagnostics.
- `toUiState(h)`: `DeviceHealth -> HealthUiState`. Maps raw statuses into UI color badges and health percentages.
- `criticalIssues(h)`: `DeviceHealth -> List<HealthIssue>`. Extracts items requiring immediate attention.

### Execution Flow
```text
device health telemetry → map to diagnostic models → render component checklist → publish critical events
```

### Must NOT Implement
- No automatic hardware repair routines.
- No motor tuning or current limit alterations.
- No invention of custom telemetry protocols.

---

## 4. Complete Skeleton Code for Every File

### `domain/HealthRepository.kt`
```kotlin
package com.solarrobo.feature.health.domain

import com.solarrobo.core.contracts.DeviceHealth
import kotlinx.coroutines.flow.Flow

interface HealthRepository {
    fun observeHealth(): Flow<DeviceHealth>
}
```

### `data/HealthRepositoryImpl.kt`
```kotlin
package com.solarrobo.feature.health.data

import com.solarrobo.core.contracts.DeviceHealth
import com.solarrobo.core.contracts.HealthStatus
import com.solarrobo.feature.health.domain.HealthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepositoryImpl @Inject constructor() : HealthRepository {
    override fun observeHealth(): Flow<DeviceHealth> = flowOf(
        DeviceHealth(
            overallStatus = HealthStatus.HEALTHY,
            motorStatus = HealthStatus.HEALTHY,
            batteryHealthPercent = 98.0f,
            solarPanelStatus = HealthStatus.HEALTHY,
            sensorsStatus = HealthStatus.HEALTHY,
            cameraStatus = HealthStatus.HEALTHY,
            controllerTemperatureC = 34.5f,
            issues = emptyList(),
            timestamp = System.currentTimeMillis()
        )
    )
}
```

### `HealthModule.kt`
```kotlin
package com.solarrobo.feature.health

import com.solarrobo.feature.health.data.HealthRepositoryImpl
import com.solarrobo.feature.health.domain.HealthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HealthModule {
    @Binds
    @Singleton
    abstract fun bindHealthRepository(impl: HealthRepositoryImpl): HealthRepository
}
```

### `presentation/HealthUiState.kt`
```kotlin
package com.solarrobo.feature.health.presentation

import com.solarrobo.core.contracts.DeviceHealth

data class HealthUiState(
    val health: DeviceHealth? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
```

### `presentation/HealthViewModel.kt`
```kotlin
package com.solarrobo.feature.health.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.feature.health.domain.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val repository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthUiState(isLoading = true))
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeHealth().collect { h ->
                _uiState.update { it.copy(health = h, isLoading = false) }
            }
        }
    }
}
```

### `presentation/HealthScreen.kt`
```kotlin
package com.solarrobo.feature.health.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.feature.health.components.HealthCheckItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(
    viewModel: HealthViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Health & Diagnostics") }) },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.health?.let { h ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Overall System: ${h.overallStatus.name}", style = MaterialTheme.typography.titleMedium)
                            Text("Controller Temp: ${h.controllerTemperatureC.toInt()} °C")
                            Text("Battery Health: ${h.batteryHealthPercent.toInt()}%")
                        }
                    }
                }
                item { HealthCheckItem(name = "Actuator Motors", status = h.motorStatus) }
                item { HealthCheckItem(name = "Solar Array", status = h.solarPanelStatus) }
                item { HealthCheckItem(name = "Sensor Cluster", status = h.sensorsStatus) }
                item { HealthCheckItem(name = "Camera Hardware", status = h.cameraStatus) }
            }
        }
    }
}
```

### `components/HealthCheckItem.kt`
```kotlin
package com.solarrobo.feature.health.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.core.contracts.HealthStatus

@Composable
fun HealthCheckItem(name: String, status: HealthStatus, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(status.name, style = MaterialTheme.typography.titleSmall)
        }
    }
}
```

### `mock/FakeHealthRepository.kt`
```kotlin
package com.solarrobo.feature.health.mock

import com.solarrobo.core.contracts.DeviceHealth
import com.solarrobo.core.contracts.HealthStatus
import com.solarrobo.feature.health.domain.HealthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeHealthRepository : HealthRepository {
    override fun observeHealth(): Flow<DeviceHealth> = flowOf(
        DeviceHealth(
            HealthStatus.HEALTHY,
            HealthStatus.HEALTHY,
            95f,
            HealthStatus.HEALTHY,
            HealthStatus.HEALTHY,
            HealthStatus.HEALTHY,
            32f,
            emptyList(),
            System.currentTimeMillis()
        )
    )
}
```

### `src/test/java/com/solarrobo/feature/health/HealthViewModelTest.kt`
```kotlin
package com.solarrobo.feature.health

import com.solarrobo.feature.health.mock.FakeHealthRepository
import com.solarrobo.feature.health.presentation.HealthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HealthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: HealthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = HealthViewModel(FakeHealthRepository())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsHealth() = runTest {
        dispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.health)
    }
}
```

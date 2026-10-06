# Module Specification: Environment (`feature/environment`)

## 1. Exact Task & Responsibility
Display current local atmospheric measurements gathered by the Solar Robo sensor array: ambient temperature, relative humidity, direct solar irradiance (lux), wind speed (m/s), precipitation/rain status, and back-of-panel thermal sensor.

---

## 2. Exact Files & Directory Layout
```text
feature/environment/
├── build.gradle.kts
├── src/
│   ├── main/java/com/solarrobo/feature/environment/
│   │   ├── EnvironmentModule.kt
│   │   ├── domain/
│   │   │   └── EnvironmentRepository.kt
│   │   ├── data/
│   │   │   └── EnvironmentRepositoryImpl.kt
│   │   ├── presentation/
│   │   │   ├── EnvironmentViewModel.kt
│   │   │   ├── EnvironmentUiState.kt
│   │   │   └── EnvironmentScreen.kt
│   │   ├── components/
│   │   │   ├── EnvironmentCard.kt
│   │   │   ├── ConditionCard.kt
│   │   │   └── WeatherRiskCard.kt
│   │   └── mock/
│   │       └── FakeEnvironmentRepository.kt
│   └── test/java/com/solarrobo/feature/environment/
│       └── EnvironmentViewModelTest.kt
```

---

## 3. Module I/O & Boundaries

| Dir | Resource | Source / Consumer | Meaning & Boundary Rule |
|:---:|:---------|:------------------|:------------------------|
| **IN** | `EnvironmentSnapshot`| Device / Simulator | Temperature, humidity, lux, wind, rain, panel temp. |
| **OUT**| `EnvironmentUiState` | Jetpack Compose UI | Formatted UI conditions and weather risk indicators. |
| **OUT**| `ConditionEvent` | Event Bus | Significant change event (e.g. rain onset, wind surge). |

### Function-Level Tasks
- `observeEnvironment()`: `none -> Flow<EnvironmentSnapshot>`. Streams real-time telemetry from onboard weather sensors.
- `toUiState(s)`: `EnvironmentSnapshot -> EnvironmentUiState`. Maps raw floating point sensor values to readable units (°C, %, Lux, m/s).
- `detectChange(p, c)`: `EnvironmentSnapshot + EnvironmentSnapshot -> Boolean`. Identifies significant transitions (e.g. rain onset or wind > 10 m/s).

### Execution Flow
```text
sensor snapshot → validate/normalize → format units → render condition cards → emit condition events
```

### Must NOT Implement
- No long-term weather forecasting engine.
- No external 3rd-party weather APIs unless explicitly configured.
- No panel motion commands.
- No safety threshold definitions in the UI layer (safety thresholds belong to `:feature:safety`).

---

## 4. Complete Skeleton Code for Every File

### `domain/EnvironmentRepository.kt`
```kotlin
package com.solarrobo.feature.environment.domain

import com.solarrobo.core.contracts.EnvironmentSnapshot
import kotlinx.coroutines.flow.Flow

interface EnvironmentRepository {
    fun observeEnvironment(): Flow<EnvironmentSnapshot>
}
```

### `data/EnvironmentRepositoryImpl.kt`
```kotlin
package com.solarrobo.feature.environment.data

import com.solarrobo.core.contracts.EnvironmentSnapshot
import com.solarrobo.feature.environment.domain.EnvironmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnvironmentRepositoryImpl @Inject constructor() : EnvironmentRepository {
    override fun observeEnvironment(): Flow<EnvironmentSnapshot> = flowOf(
        EnvironmentSnapshot(
            temperatureC = 27.4f,
            humidityPercent = 48.0f,
            lightLux = 78500.0f,
            windSpeedMps = 4.2f,
            rainDetected = false,
            panelTemperatureC = 41.2f,
            timestamp = System.currentTimeMillis()
        )
    )
}
```

### `EnvironmentModule.kt`
```kotlin
package com.solarrobo.feature.environment

import com.solarrobo.feature.environment.data.EnvironmentRepositoryImpl
import com.solarrobo.feature.environment.domain.EnvironmentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EnvironmentModule {
    @Binds
    @Singleton
    abstract fun bindEnvironmentRepository(impl: EnvironmentRepositoryImpl): EnvironmentRepository
}
```

### `presentation/EnvironmentUiState.kt`
```kotlin
package com.solarrobo.feature.environment.presentation

import com.solarrobo.core.contracts.EnvironmentSnapshot

data class EnvironmentUiState(
    val snapshot: EnvironmentSnapshot? = null,
    val isWindRisky: Boolean = false,
    val isRaining: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
```

### `presentation/EnvironmentViewModel.kt`
```kotlin
package com.solarrobo.feature.environment.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.feature.environment.domain.EnvironmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EnvironmentViewModel @Inject constructor(
    private val repository: EnvironmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EnvironmentUiState(isLoading = true))
    val uiState: StateFlow<EnvironmentUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeEnvironment().collect { snap ->
                _uiState.update {
                    it.copy(
                        snapshot = snap,
                        isWindRisky = snap.windSpeedMps >= 12.0f,
                        isRaining = snap.rainDetected,
                        isLoading = false
                    )
                }
            }
        }
    }
}
```

### `presentation/EnvironmentScreen.kt`
```kotlin
package com.solarrobo.feature.environment.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.feature.environment.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvironmentScreen(
    viewModel: EnvironmentViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Local Environment") }) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            state.snapshot?.let { snap ->
                WeatherRiskCard(isWindRisky = state.isWindRisky, isRaining = state.isRaining)
                ConditionCard(title = "Solar Irradiance", value = "${snap.lightLux.toInt()} Lux", subtitle = "Direct Sunlight")
                ConditionCard(title = "Panel Temperature", value = "${snap.panelTemperatureC.toInt()} °C", subtitle = "Ambient: ${snap.temperatureC.toInt()} °C")
                ConditionCard(title = "Wind & Humidity", value = "${snap.windSpeedMps} m/s", subtitle = "Humidity: ${snap.humidityPercent.toInt()}%")
            }
        }
    }
}
```

### `components/ConditionCard.kt`
```kotlin
package com.solarrobo.feature.environment.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ConditionCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

### `components/WeatherRiskCard.kt`
```kotlin
package com.solarrobo.feature.environment.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WeatherRiskCard(isWindRisky: Boolean, isRaining: Boolean, modifier: Modifier = Modifier) {
    if (isWindRisky || isRaining) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Adverse Weather Warning", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                if (isWindRisky) Text("• High wind speeds detected: automated stow active", style = MaterialTheme.typography.bodySmall)
                if (isRaining) Text("• Rain detected: moisture protection active", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
```

### `mock/FakeEnvironmentRepository.kt`
```kotlin
package com.solarrobo.feature.environment.mock

import com.solarrobo.core.contracts.EnvironmentSnapshot
import com.solarrobo.feature.environment.domain.EnvironmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeEnvironmentRepository : EnvironmentRepository {
    override fun observeEnvironment(): Flow<EnvironmentSnapshot> = flowOf(
        EnvironmentSnapshot(26f, 45f, 80000f, 3.5f, false, 38f, System.currentTimeMillis())
    )
}
```

### `src/test/java/com/solarrobo/feature/environment/EnvironmentViewModelTest.kt`
```kotlin
package com.solarrobo.feature.environment

import com.solarrobo.feature.environment.mock.FakeEnvironmentRepository
import com.solarrobo.feature.environment.presentation.EnvironmentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EnvironmentViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: EnvironmentViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = EnvironmentViewModel(FakeEnvironmentRepository())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsSnapshot() = runTest {
        dispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.snapshot)
    }
}
```

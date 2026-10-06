# Module Specification: Developer Simulator (`feature/simulator`)

## 1. Exact Task & Responsibility
Deterministic fake simulation engine producing authentic production contracts (`RoboSnapshot`, `EnergySnapshot`, `EnvironmentSnapshot`, `SafetyEvent`). Allows full Android app development, Compose `@Preview` rendering, unit testing, and edge-case validation without physical ESP32 or sensor hardware.

---

## 2. Exact Files & Directory Layout
```text
feature/simulator/
├── build.gradle.kts
├── src/
│   ├── main/java/com/solarrobo/feature/simulator/
│   │   ├── SimulatorModule.kt
│   │   ├── domain/
│   │   │   ├── SimulatorRepository.kt
│   │   │   └── Scenario.kt
│   │   ├── data/
│   │   │   └── SimulatorRepositoryImpl.kt
│   │   ├── presentation/
│   │   │   ├── SimulatorViewModel.kt
│   │   │   ├── SimulatorUiState.kt
│   │   │   └── SimulatorScreen.kt
│   │   ├── components/
│   │   │   ├── SimulatorCard.kt
│   │   │   └── SimulationControl.kt
│   │   └── mock/
│   │       └── FakeSimulatorRepository.kt
│   └── test/java/com/solarrobo/feature/simulator/
│       └── SimulatorViewModelTest.kt
```

---

## 3. Module I/O & Boundaries

| Dir | Resource | Source / Consumer | Meaning & Boundary Rule |
|:---:|:---------|:------------------|:------------------------|
| **IN** | `ScenarioConfig` | Developer UI | Sun position, wind speed, rain toggle, fault injection. |
| **IN** | `tick(dt)` | Engine / Clock | Deterministic simulated time advancement step. |
| **OUT**| `RoboSnapshot` | Core Device Boundary | Fake robotic tracker state conforming to contracts. |
| **OUT**| `EnergySnapshot`| Core Contracts | Fake photovoltaic generation and battery discharge. |
| **OUT**| `EnvironmentSnapshot`| Core Contracts | Fake environmental and weather conditions. |
| **OUT**| `SafetyEvent` | Event Bus | Injected safety incidents (wind warning, motor stall). |

### Scenarios
```kotlin
enum class Scenario {
    SUNNY_NORMAL,
    LOW_LIGHT,
    HIGH_WIND,
    RAIN,
    MOTOR_JAM,
    BATTERY_LOW,
    DEVICE_OFFLINE
}
```

### Function-Level Tasks
- `applyScenario(s)`: `Scenario -> Unit`. Configures the simulated world state.
- `tick(dt)`: `Long -> SimState`. Advances tracker physics, sun vector, and battery levels deterministically.
- `buildSnapshots(s)`: `SimState -> StandardSnapshots`. Emits canonical types without deviations.
- `injectFault(type)`: `FaultType -> SafetyEvent`. Triggers controlled hardware error conditions.

### Execution Flow
```text
select scenario → initialize sim state → tick clock → emit snapshots to flows → test UI & Safety reactions
```

### Must NOT Implement
- Never connect to real ESP32 hardware or Bluetooth sockets.
- Never replace or overwrite the production `RoboDevice` implementation in release builds.
- No non-deterministic random number generators in test verification runs.
- No changes to production safety thresholds.

---

## 4. Complete Skeleton Code for Every File

### `domain/Scenario.kt`
```kotlin
package com.solarrobo.feature.simulator.domain

enum class Scenario {
    SUNNY_NORMAL,
    LOW_LIGHT,
    HIGH_WIND,
    RAIN,
    MOTOR_JAM,
    BATTERY_LOW,
    DEVICE_OFFLINE
}
```

### `domain/SimulatorRepository.kt`
```kotlin
package com.solarrobo.feature.simulator.domain

import com.solarrobo.core.contracts.*
import kotlinx.coroutines.flow.Flow

interface SimulatorRepository {
    fun getRoboSnapshot(): Flow<RoboSnapshot>
    fun getEnergySnapshot(): Flow<EnergySnapshot>
    fun getEnvironmentSnapshot(): Flow<EnvironmentSnapshot>
    suspend fun applyScenario(scenario: Scenario)
    suspend fun injectFault(code: String, message: String): SafetyEvent
}
```

### `data/SimulatorRepositoryImpl.kt`
```kotlin
package com.solarrobo.feature.simulator.data

import com.solarrobo.core.contracts.*
import com.solarrobo.feature.simulator.domain.Scenario
import com.solarrobo.feature.simulator.domain.SimulatorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimulatorRepositoryImpl @Inject constructor() : SimulatorRepository {

    private val activeScenario = MutableStateFlow(Scenario.SUNNY_NORMAL)

    private val roboState = MutableStateFlow(
        RoboSnapshot("SIM-01", "Simulated Robo", true, RoboMode.OPTIMIZING, 35f, 35f, 310f, 92f, System.currentTimeMillis())
    )
    private val energyState = MutableStateFlow(
        EnergySnapshot(310f, 45f, 92f, 265f, 20f, GridState.EXPORT_READY, System.currentTimeMillis())
    )
    private val envState = MutableStateFlow(
        EnvironmentSnapshot(26f, 45f, 75000f, 4.5f, false, 40f, System.currentTimeMillis())
    )

    override fun getRoboSnapshot(): Flow<RoboSnapshot> = roboState.asStateFlow()
    override fun getEnergySnapshot(): Flow<EnergySnapshot> = energyState.asStateFlow()
    override fun getEnvironmentSnapshot(): Flow<EnvironmentSnapshot> = envState.asStateFlow()

    override suspend fun applyScenario(scenario: Scenario) {
        activeScenario.value = scenario
        when (scenario) {
            Scenario.SUNNY_NORMAL -> {
                roboState.value = roboState.value.copy(mode = RoboMode.NORMAL, generationWatts = 320f)
                envState.value = envState.value.copy(lightLux = 85000f, windSpeedMps = 3.5f, rainDetected = false)
            }
            Scenario.HIGH_WIND -> {
                roboState.value = roboState.value.copy(mode = RoboMode.PROTECTING, targetAngleDeg = 0f)
                envState.value = envState.value.copy(windSpeedMps = 18.2f)
            }
            Scenario.RAIN -> {
                roboState.value = roboState.value.copy(mode = RoboMode.PROTECTING)
                envState.value = envState.value.copy(rainDetected = true, lightLux = 15000f)
            }
            Scenario.BATTERY_LOW -> {
                roboState.value = roboState.value.copy(batteryPercent = 8f, mode = RoboMode.CONSERVING)
            }
            Scenario.MOTOR_JAM -> {
                roboState.value = roboState.value.copy(mode = RoboMode.FAULT)
            }
            Scenario.DEVICE_OFFLINE -> {
                roboState.value = roboState.value.copy(connected = false)
            }
            Scenario.LOW_LIGHT -> {
                roboState.value = roboState.value.copy(generationWatts = 40f)
                envState.value = envState.value.copy(lightLux = 5000f)
            }
        }
    }

    override suspend fun injectFault(code: String, message: String): SafetyEvent {
        return SafetyEvent(UUID.randomUUID().toString(), SafetyLevel.FAULT, code, message, System.currentTimeMillis())
    }
}
```

### `SimulatorModule.kt`
```kotlin
package com.solarrobo.feature.simulator

import com.solarrobo.feature.simulator.data.SimulatorRepositoryImpl
import com.solarrobo.feature.simulator.domain.SimulatorRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SimulatorModule {
    @Binds
    @Singleton
    abstract fun bindSimulatorRepository(impl: SimulatorRepositoryImpl): SimulatorRepository
}
```

### `presentation/SimulatorUiState.kt`
```kotlin
package com.solarrobo.feature.simulator.presentation

import com.solarrobo.core.contracts.EnvironmentSnapshot
import com.solarrobo.core.contracts.RoboSnapshot
import com.solarrobo.feature.simulator.domain.Scenario

data class SimulatorUiState(
    val currentScenario: Scenario = Scenario.SUNNY_NORMAL,
    val robo: RoboSnapshot? = null,
    val env: EnvironmentSnapshot? = null,
    val lastInjectedFault: String? = null
)
```

### `presentation/SimulatorViewModel.kt`
```kotlin
package com.solarrobo.feature.simulator.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.feature.simulator.domain.Scenario
import com.solarrobo.feature.simulator.domain.SimulatorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SimulatorViewModel @Inject constructor(
    private val repository: SimulatorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SimulatorUiState())
    val uiState: StateFlow<SimulatorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getRoboSnapshot().collect { r ->
                _uiState.update { it.copy(robo = r) }
            }
        }
        viewModelScope.launch {
            repository.getEnvironmentSnapshot().collect { e ->
                _uiState.update { it.copy(env = e) }
            }
        }
    }

    fun selectScenario(scenario: Scenario) {
        viewModelScope.launch {
            repository.applyScenario(scenario)
            _uiState.update { it.copy(currentScenario = scenario) }
        }
    }

    fun triggerJamFault() {
        viewModelScope.launch {
            val event = repository.injectFault("MOTOR_STALL_01", "Azimuth gear overcurrent threshold exceeded.")
            _uiState.update { it.copy(lastInjectedFault = event.message) }
        }
    }
}
```

### `presentation/SimulatorScreen.kt`
```kotlin
package com.solarrobo.feature.simulator.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.feature.simulator.components.SimulationControl
import com.solarrobo.feature.simulator.domain.Scenario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatorScreen(
    viewModel: SimulatorViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Developer Hardware Simulator") }) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SimulationControl(
                currentScenario = state.currentScenario,
                onScenarioSelect = { viewModel.selectScenario(it) }
            )
            state.robo?.let { r ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Simulated Robo Output", style = MaterialTheme.typography.titleMedium)
                        Text("Mode: ${r.mode} | Angle: ${r.panelAngleDeg}° | Battery: ${r.batteryPercent.toInt()}%")
                    }
                }
            }
            Button(
                onClick = { viewModel.triggerJamFault() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Inject Motor Jam Fault")
            }
        }
    }
}
```

### `components/SimulationControl.kt`
```kotlin
package com.solarrobo.feature.simulator.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.feature.simulator.domain.Scenario

@Composable
fun SimulationControl(
    currentScenario: Scenario,
    onScenarioSelect: (Scenario) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Simulated World Scenarios", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Scenario.entries.chunked(2).forEach { rowItems ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowItems.forEach { sc ->
                        FilterChip(
                            selected = currentScenario == sc,
                            onClick = { onScenarioSelect(sc) },
                            label = { Text(sc.name) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
```

### `mock/FakeSimulatorRepository.kt`
```kotlin
package com.solarrobo.feature.simulator.mock

import com.solarrobo.core.contracts.*
import com.solarrobo.feature.simulator.domain.Scenario
import com.solarrobo.feature.simulator.domain.SimulatorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

class FakeSimulatorRepository : SimulatorRepository {
    override fun getRoboSnapshot(): Flow<RoboSnapshot> = flowOf(
        RoboSnapshot("SIM", "Mock", true, RoboMode.NORMAL, 0f, 0f, 100f, 100f, 0L)
    )
    override fun getEnergySnapshot(): Flow<EnergySnapshot> = flowOf(
        EnergySnapshot(100f, 10f, 100f, 90f, 20f, GridState.EXPORT_READY, 0L)
    )
    override fun getEnvironmentSnapshot(): Flow<EnvironmentSnapshot> = flowOf(
        EnvironmentSnapshot(20f, 50f, 50000f, 1f, false, 25f, 0L)
    )
    override suspend fun applyScenario(scenario: Scenario) {}
    override suspend fun injectFault(code: String, message: String): SafetyEvent = SafetyEvent(
        UUID.randomUUID().toString(), SafetyLevel.FAULT, code, message, 0L
    )
}
```

### `src/test/java/com/solarrobo/feature/simulator/SimulatorViewModelTest.kt`
```kotlin
package com.solarrobo.feature.simulator

import com.solarrobo.feature.simulator.domain.Scenario
import com.solarrobo.feature.simulator.mock.FakeSimulatorRepository
import com.solarrobo.feature.simulator.presentation.SimulatorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SimulatorViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SimulatorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = SimulatorViewModel(FakeSimulatorRepository())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun selectScenario_updatesState() = runTest {
        viewModel.selectScenario(Scenario.HIGH_WIND)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(Scenario.HIGH_WIND, viewModel.uiState.value.currentScenario)
    }
}
```

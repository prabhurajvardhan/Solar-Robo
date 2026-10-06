# Module Specification: Safety & Emergency (`feature/safety`)

## 1. Exact Task & Responsibility
Deterministic physical-command gate, emergency stop enforcement, automatic high-wind stow, rain shelter mode, and incident audit ledger. This module is the ultimate authority determining whether physical commands are dispatched to hardware.

---

## 2. Exact Files & Directory Layout
```text
feature/safety/
├── build.gradle.kts
├── src/
│   ├── main/java/com/solarrobo/feature/safety/
│   │   ├── SafetyModule.kt
│   │   ├── domain/
│   │   │   ├── SafetyRepository.kt
│   │   │   └── SafetyPolicy.kt
│   │   ├── data/
│   │   │   └── SafetyRepositoryImpl.kt
│   │   ├── presentation/
│   │   │   ├── SafetyViewModel.kt
│   │   │   ├── SafetyUiState.kt
│   │   │   └── SafetyScreen.kt
│   │   ├── components/
│   │   │   ├── SafetyCard.kt
│   │   │   ├── SafetyBanner.kt
│   │   │   ├── EmergencyStopButton.kt
│   │   │   └── IncidentList.kt
│   │   └── mock/
│   │       └── FakeSafetyRepository.kt
│   └── test/java/com/solarrobo/feature/safety/
│       ├── SafetyViewModelTest.kt
│       └── SafetyPolicyTest.kt
```

---

## 3. Module I/O & Boundaries

| Dir | Resource | Source / Consumer | Meaning & Boundary Rule |
|:---:|:---------|:------------------|:------------------------|
| **IN** | `RoboCommand` | Control boundary | Requested movement command. |
| **IN** | `EnvironmentSnapshot` | Environment boundary | Wind speed, rain sensors, temperature. |
| **IN** | `DeviceHealth` | Health / Device | Hardware faults, motor jams, battery level. |
| **OUT**| `SafetyDecision` | Control / Device | `ALLOW`, `BLOCK`, or `MODIFY` command decision. |
| **OUT**| `SafetyEvent` | Event Bus / Room | Incident logged with timestamp and code. |
| **OUT**| Safe Command | Device Adapter | `StopMotion` or `SafePosition` dispatched directly. |

### Function-Level Tasks
- `evaluate(c, ctx)`: `RoboCommand + SafetyContext -> SafetyDecision`. Applies deterministic boolean rules.
- `emergencyStop(reason)`: `String -> CommandResult`. Halts actuator motor immediately and generates emergency safety event.
- `safePosition(reason)`: `String -> CommandResult`. Directs tracker to flat 0° stow position and logs event.
- `acknowledge(id)`: `String -> Unit`. Marks safety event acknowledged in local persistence.

### Execution Flow
```text
incoming command → evaluate deterministic policy → ALLOW / BLOCK / MODIFY → dispatch safe action → log incident
```

### Must NOT Implement
- No LLM/AI safety decisions (must remain strictly deterministic Kotlin code).
- No silent runtime threshold modifications.
- No duplicate ownership of sensor telemetry.

---

## 4. Complete Skeleton Code for Every File

### `domain/SafetyPolicy.kt`
```kotlin
package com.solarrobo.feature.safety.domain

import com.solarrobo.core.contracts.*
import javax.inject.Inject

data class SafetyContext(
    val environment: EnvironmentSnapshot,
    val health: DeviceHealth
)

class SafetyPolicy @Inject constructor() {
    companion object {
        const val MAX_SAFE_WIND_SPEED_MPS = 15.0f // 54 km/h
        const val MIN_BATTERY_RESERVE_PERCENT = 10.0f
        const val MAX_SAFE_ANGLE_DEG = 85.0f
        const val MIN_SAFE_ANGLE_DEG = -85.0f
    }

    fun evaluate(command: RoboCommand, context: SafetyContext): SafetyDecision {
        // Rule 1: High Wind Stow Override
        if (context.environment.windSpeedMps > MAX_SAFE_WIND_SPEED_MPS) {
            return SafetyDecision.Modify(RoboCommand.SafePosition, "High wind speed (${context.environment.windSpeedMps} m/s) forces safe stow.")
        }

        // Rule 2: Critical Hardware Fault Block
        if (context.health.motorStatus == HealthStatus.CRITICAL || context.health.motorStatus == HealthStatus.FAULT) {
            return SafetyDecision.Block("Motor hardware fault reported; motion prohibited.")
        }

        // Rule 3: Low Battery Conservation
        if (context.health.batteryHealthPercent < MIN_BATTERY_RESERVE_PERCENT && command is RoboCommand.MoveToAngle) {
            return SafetyDecision.Block("Battery below safe threshold (<10%). Conserving power.")
        }

        // Rule 4: Mechanical Bounds Check
        if (command is RoboCommand.MoveToAngle) {
            if (command.angleDeg > MAX_SAFE_ANGLE_DEG || command.angleDeg < MIN_SAFE_ANGLE_DEG) {
                val clamped = command.angleDeg.coerceIn(MIN_SAFE_ANGLE_DEG, MAX_SAFE_ANGLE_DEG)
                return SafetyDecision.Modify(RoboCommand.MoveToAngle(clamped), "Angle clamped to mechanical limit.")
            }
        }

        return SafetyDecision.Allow
    }
}
```

### `domain/SafetyRepository.kt`
```kotlin
package com.solarrobo.feature.safety.domain

import com.solarrobo.core.contracts.CommandResult
import com.solarrobo.core.contracts.SafetyDecision
import com.solarrobo.core.contracts.SafetyEvent
import kotlinx.coroutines.flow.Flow

interface SafetyRepository {
    fun observeSafetyEvents(): Flow<List<SafetyEvent>>
    suspend fun evaluateAndExecute(command: com.solarrobo.core.contracts.RoboCommand): SafetyDecision
    suspend fun triggerEmergencyStop(reason: String): CommandResult
    suspend fun requestSafeStow(reason: String): CommandResult
    suspend fun acknowledgeIncident(id: String)
}
```

### `data/SafetyRepositoryImpl.kt`
```kotlin
package com.solarrobo.feature.safety.data

import com.solarrobo.core.contracts.*
import com.solarrobo.core.device.RoboDevice
import com.solarrobo.feature.safety.domain.SafetyPolicy
import com.solarrobo.feature.safety.domain.SafetyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SafetyRepositoryImpl @Inject constructor(
    private val device: RoboDevice,
    private val safetyPolicy: SafetyPolicy
) : SafetyRepository {

    private val events = MutableStateFlow<List<SafetyEvent>>(emptyList())

    override fun observeSafetyEvents(): Flow<List<SafetyEvent>> = events.asStateFlow()

    override suspend fun evaluateAndExecute(command: RoboCommand): SafetyDecision {
        // Deterministic check and execution
        return SafetyDecision.Allow
    }

    override suspend fun triggerEmergencyStop(reason: String): CommandResult {
        val event = SafetyEvent(UUID.randomUUID().toString(), SafetyLevel.EMERGENCY, "E_STOP", reason, System.currentTimeMillis())
        events.value = events.value + event
        return device.sendCommand(RoboCommand.StopMotion)
    }

    override suspend fun requestSafeStow(reason: String): CommandResult {
        val event = SafetyEvent(UUID.randomUUID().toString(), SafetyLevel.PROTECTING, "SAFE_STOW", reason, System.currentTimeMillis())
        events.value = events.value + event
        return device.sendCommand(RoboCommand.SafePosition)
    }

    override suspend fun acknowledgeIncident(id: String) {
        events.value = events.value.map { if (it.id == id) it.copy(acknowledged = true) else it }
    }
}
```

### `SafetyModule.kt`
```kotlin
package com.solarrobo.feature.safety

import com.solarrobo.feature.safety.data.SafetyRepositoryImpl
import com.solarrobo.feature.safety.domain.SafetyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SafetyModule {
    @Binds
    @Singleton
    abstract fun bindSafetyRepository(impl: SafetyRepositoryImpl): SafetyRepository
}
```

### `presentation/SafetyUiState.kt`
```kotlin
package com.solarrobo.feature.safety.presentation

import com.solarrobo.core.contracts.SafetyEvent
import com.solarrobo.core.contracts.SafetyLevel

data class SafetyUiState(
    val currentLevel: SafetyLevel = SafetyLevel.NORMAL,
    val activeIncidents: List<SafetyEvent> = emptyList(),
    val isEmergencyActive: Boolean = false
)
```

### `presentation/SafetyViewModel.kt`
```kotlin
package com.solarrobo.feature.safety.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.core.contracts.SafetyLevel
import com.solarrobo.feature.safety.domain.SafetyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SafetyViewModel @Inject constructor(
    private val repository: SafetyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SafetyUiState())
    val uiState: StateFlow<SafetyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeSafetyEvents().collect { list ->
                val unacknowledged = list.filter { !it.acknowledged }
                val highest = unacknowledged.maxByOrNull { it.level.ordinal }?.level ?: SafetyLevel.NORMAL
                _uiState.update {
                    it.copy(
                        currentLevel = highest,
                        activeIncidents = unacknowledged,
                        isEmergencyActive = highest == SafetyLevel.EMERGENCY
                    )
                }
            }
        }
    }

    fun triggerEmergencyStop() {
        viewModelScope.launch {
            repository.triggerEmergencyStop("Manual E-Stop button pressed by operator.")
        }
    }

    fun stowSafePosition() {
        viewModelScope.launch {
            repository.requestSafeStow("Operator triggered safe stow position.")
        }
    }

    fun acknowledge(id: String) {
        viewModelScope.launch {
            repository.acknowledgeIncident(id)
        }
    }
}
```

### `presentation/SafetyScreen.kt`
```kotlin
package com.solarrobo.feature.safety.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.feature.safety.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyScreen(
    viewModel: SafetyViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Safety & Protection") }) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SafetyBanner(level = state.currentLevel)
            EmergencyStopButton(
                isPressed = state.isEmergencyActive,
                onClick = { viewModel.triggerEmergencyStop() }
            )
            IncidentList(
                incidents = state.activeIncidents,
                onAcknowledge = { viewModel.acknowledge(it) }
            )
        }
    }
}
```

### `components/EmergencyStopButton.kt`
```kotlin
package com.solarrobo.feature.safety.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EmergencyStopButton(
    isPressed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = modifier.fillMaxWidth().height(64.dp)
    ) {
        Text("EMERGENCY STOP (HALT ALL MOTORS)", style = MaterialTheme.typography.titleMedium)
    }
}
```

### `components/SafetyBanner.kt`
```kotlin
package com.solarrobo.feature.safety.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.core.contracts.SafetyLevel

@Composable
fun SafetyBanner(level: SafetyLevel, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (level == SafetyLevel.NORMAL) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Safety Status: ${level.name}", style = MaterialTheme.typography.titleLarge)
            Text("Deterministic hardware protection gate active.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
```

### `components/IncidentList.kt`
```kotlin
package com.solarrobo.feature.safety.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.core.contracts.SafetyEvent

@Composable
fun IncidentList(
    incidents: List<SafetyEvent>,
    onAcknowledge: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Active Incidents (${incidents.size})", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (incidents.isEmpty()) {
                Text("No safety incidents reported. System nominal.", style = MaterialTheme.typography.bodySmall)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(incidents) { inc ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${inc.code}: ${inc.message}", modifier = Modifier.weight(1f))
                            TextButton(onClick = { onAcknowledge(inc.id) }) {
                                Text("Ack")
                            }
                        }
                    }
                }
            }
        }
    }
}
```

### `mock/FakeSafetyRepository.kt`
```kotlin
package com.solarrobo.feature.safety.mock

import com.solarrobo.core.contracts.*
import com.solarrobo.feature.safety.domain.SafetyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

class FakeSafetyRepository : SafetyRepository {
    override fun observeSafetyEvents(): Flow<List<SafetyEvent>> = flowOf(emptyList())
    override suspend fun evaluateAndExecute(command: RoboCommand): SafetyDecision = SafetyDecision.Allow
    override suspend fun triggerEmergencyStop(reason: String): CommandResult = CommandResult(true, UUID.randomUUID().toString())
    override suspend fun requestSafeStow(reason: String): CommandResult = CommandResult(true, UUID.randomUUID().toString())
    override suspend fun acknowledgeIncident(id: String) {}
}
```

### `src/test/java/com/solarrobo/feature/safety/SafetyPolicyTest.kt`
```kotlin
package com.solarrobo.feature.safety

import com.solarrobo.core.contracts.*
import com.solarrobo.feature.safety.domain.SafetyContext
import com.solarrobo.feature.safety.domain.SafetyPolicy
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyPolicyTest {
    private val policy = SafetyPolicy()

    @Test
    fun highWind_forcesSafeStow() {
        val env = EnvironmentSnapshot(25f, 50f, 60000f, 18.5f, false, 35f, 1000L)
        val health = DeviceHealth(HealthStatus.HEALTHY, HealthStatus.HEALTHY, 90f, HealthStatus.HEALTHY, HealthStatus.HEALTHY, HealthStatus.HEALTHY, 30f, emptyList(), 1000L)
        val decision = policy.evaluate(RoboCommand.MoveToAngle(30f), SafetyContext(env, health))
        assertTrue(decision is SafetyDecision.Modify)
    }
}
```

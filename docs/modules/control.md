# Module Specification: Robo Control (`feature/control`)

## 1. Exact Task & Responsibility
Manual and automatic tracker control UI facilitating panel angle adjustment, mode toggling, motion halt, and parking in the safe stow position. All commands MUST route through the Safety Boundary before hardware dispatch.

---

## 2. Exact Files & Directory Layout
```text
feature/control/
├── build.gradle.kts
├── src/
│   ├── main/java/com/solarrobo/feature/control/
│   │   ├── ControlModule.kt
│   │   ├── domain/
│   │   │   └── ControlRepository.kt
│   │   ├── data/
│   │   │   └── ControlRepositoryImpl.kt
│   │   ├── presentation/
│   │   │   ├── ControlViewModel.kt
│   │   │   ├── ControlUiState.kt
│   │   │   └── ControlScreen.kt
│   │   ├── components/
│   │   │   ├── ControlCard.kt
│   │   │   ├── AngleControl.kt
│   │   │   ├── ModeToggle.kt
│   │   │   └── MotionStatus.kt
│   │   └── mock/
│   │       └── FakeControlRepository.kt
│   └── test/java/com/solarrobo/feature/control/
│       └── ControlViewModelTest.kt
```

---

## 3. Module I/O & Boundaries

| Dir | Resource | Source / Consumer | Meaning & Boundary Rule |
|:---:|:---------|:------------------|:------------------------|
| **IN** | `RoboSnapshot` | Device / Repository | Current panel angle, target angle, and mode. |
| **IN** | Angle / Action | User Interaction (UI) | Desired angle slider or stop button tap. |
| **OUT**| `RoboCommand` | Safety Boundary | Typed command submitted for safety validation. |
| **OUT**| `CommandResult`| Device Adapter | Acknowledged execution result. |

### Function-Level Tasks
- `validateAngle(a)`: `Float -> Result<Float>`. Validates angle is within mechanical envelope [-90.0f, +90.0f].
- `createMoveCommand(a)`: `Float -> RoboCommand`. Packages validated angle into `RoboCommand.MoveToAngle(a)`.
- `sendMove(a)`: `Float -> CommandResult`. Dispatches command to safety gateway.
- `stopMotion()`: `none -> CommandResult`. Sends immediate `RoboCommand.StopMotion`.
- `safePosition()`: `none -> CommandResult`. Dispatches `RoboCommand.SafePosition` (0° flat stow).

### Execution Flow
```text
user angle input → validate bounds → build RoboCommand → route to Safety Gate → send to Device → observe result
```

### Must NOT Implement
- No direct GPIO or motor driver pin access.
- No low-level ESP32 serial/binary protocol handling in UI.
- No AI or LLM decisions on movement.
- NEVER bypass the safety boundary or execute unvalidated angles.

---

## 4. Complete Skeleton Code for Every File

### `ControlModule.kt`
```kotlin
package com.solarrobo.feature.control

import com.solarrobo.feature.control.data.ControlRepositoryImpl
import com.solarrobo.feature.control.domain.ControlRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ControlModule {
    @Binds
    @Singleton
    abstract fun bindControlRepository(impl: ControlRepositoryImpl): ControlRepository
}
```

### `domain/ControlRepository.kt`
```kotlin
package com.solarrobo.feature.control.domain

import com.solarrobo.core.contracts.CommandResult
import com.solarrobo.core.contracts.RoboCommand
import com.solarrobo.core.contracts.RoboSnapshot
import kotlinx.coroutines.flow.Flow

interface ControlRepository {
    fun observeTrackerState(): Flow<RoboSnapshot>
    suspend fun executeCommand(command: RoboCommand): CommandResult
}
```

### `data/ControlRepositoryImpl.kt`
```kotlin
package com.solarrobo.feature.control.data

import com.solarrobo.core.contracts.CommandResult
import com.solarrobo.core.contracts.RoboCommand
import com.solarrobo.core.contracts.RoboSnapshot
import com.solarrobo.core.device.RoboDevice
import com.solarrobo.feature.control.domain.ControlRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ControlRepositoryImpl @Inject constructor(
    private val roboDevice: RoboDevice
) : ControlRepository {
    override fun observeTrackerState(): Flow<RoboSnapshot> = roboDevice.observeSnapshot()

    override suspend fun executeCommand(command: RoboCommand): CommandResult {
        // In real wiring, this routes through SafetyGateway
        return roboDevice.sendCommand(command)
    }
}
```

### `presentation/ControlUiState.kt`
```kotlin
package com.solarrobo.feature.control.presentation

import com.solarrobo.core.contracts.RoboMode
import com.solarrobo.core.contracts.RoboSnapshot

data class ControlUiState(
    val currentAngleDeg: Float = 0f,
    val targetAngleDeg: Float = 0f,
    val mode: RoboMode = RoboMode.NORMAL,
    val isMoving: Boolean = false,
    val lastResultReason: String? = null
)
```

### `presentation/ControlViewModel.kt`
```kotlin
package com.solarrobo.feature.control.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.core.contracts.RoboCommand
import com.solarrobo.feature.control.domain.ControlRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ControlViewModel @Inject constructor(
    private val repository: ControlRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ControlUiState())
    val uiState: StateFlow<ControlUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeTrackerState().collect { snap ->
                _uiState.update {
                    it.copy(
                        currentAngleDeg = snap.panelAngleDeg,
                        targetAngleDeg = snap.targetAngleDeg,
                        mode = snap.mode,
                        isMoving = snap.panelAngleDeg != snap.targetAngleDeg
                    )
                }
            }
        }
    }

    fun setAngle(angle: Float) {
        val validated = angle.coerceIn(-90f, 90f)
        viewModelScope.launch {
            val res = repository.executeCommand(RoboCommand.MoveToAngle(validated))
            _uiState.update { it.copy(lastResultReason = res.reason) }
        }
    }

    fun emergencyStop() {
        viewModelScope.launch {
            repository.executeCommand(RoboCommand.StopMotion)
        }
    }

    fun moveToSafePosition() {
        viewModelScope.launch {
            repository.executeCommand(RoboCommand.SafePosition)
        }
    }
}
```

### `presentation/ControlScreen.kt`
```kotlin
package com.solarrobo.feature.control.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.feature.control.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
    viewModel: ControlViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Panel Control") }) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MotionStatus(currentAngle = state.currentAngleDeg, targetAngle = state.targetAngleDeg, isMoving = state.isMoving)
            AngleControl(
                targetAngle = state.targetAngleDeg,
                onAngleChange = { viewModel.setAngle(it) }
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { viewModel.moveToSafePosition() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Safe Stow (0°)")
                }
                Button(
                    onClick = { viewModel.emergencyStop() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Halt Motion")
                }
            }
        }
    }
}
```

### `components/AngleControl.kt`
```kotlin
package com.solarrobo.feature.control.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AngleControl(
    targetAngle: Float,
    onAngleChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderValue by remember(targetAngle) { mutableFloatStateOf(targetAngle) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Adjust Target Angle: ${sliderValue.toInt()}°", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onAngleChange(sliderValue) },
                valueRange = -90f..90f,
                steps = 180
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("-90° (West)", style = MaterialTheme.typography.bodySmall)
                Text("0° (Flat)", style = MaterialTheme.typography.bodySmall)
                Text("+90° (East)", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
```

### `components/MotionStatus.kt`
```kotlin
package com.solarrobo.feature.control.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MotionStatus(
    currentAngle: Float,
    targetAngle: Float,
    isMoving: Boolean,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Actuator Status", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Current Angle: ${currentAngle.toInt()}°")
            Text("Target Angle: ${targetAngle.toInt()}°")
            Text(if (isMoving) "Status: Actuating..." else "Status: Holding Position")
        }
    }
}
```

### `mock/FakeControlRepository.kt`
```kotlin
package com.solarrobo.feature.control.mock

import com.solarrobo.core.contracts.*
import com.solarrobo.feature.control.domain.ControlRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

class FakeControlRepository : ControlRepository {
    override fun observeTrackerState(): Flow<RoboSnapshot> = flowOf(
        RoboSnapshot("FAKE", "Fake Tracker", true, RoboMode.NORMAL, 15f, 15f, 250f, 90f, System.currentTimeMillis())
    )

    override suspend fun executeCommand(command: RoboCommand): CommandResult {
        return CommandResult(accepted = true, commandId = UUID.randomUUID().toString())
    }
}
```

### `src/test/java/com/solarrobo/feature/control/ControlViewModelTest.kt`
```kotlin
package com.solarrobo.feature.control

import com.solarrobo.feature.control.mock.FakeControlRepository
import com.solarrobo.feature.control.presentation.ControlViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ControlViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ControlViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = ControlViewModel(FakeControlRepository())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setAngle_withinBounds_accepted() = runTest {
        viewModel.setAngle(45f)
        dispatcher.scheduler.advanceUntilIdle()
        // Command accepted without throwing
    }
}
```

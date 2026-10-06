# Module Specification: Home Command Center (`feature/home`)

## 1. Exact Task & Responsibility
Read-only central status dashboard presenting first-glance information assembled from standardized contracts: tracker orientation, current generation watts, battery level, grid state, connection status, and active safety alerts.

---

## 2. Exact Files & Directory Layout
```text
feature/home/
├── build.gradle.kts
├── src/
│   ├── main/java/com/solarrobo/feature/home/
│   │   ├── HomeModule.kt
│   │   ├── domain/
│   │   │   └── HomeRepository.kt
│   │   ├── data/
│   │   │   └── HomeRepositoryImpl.kt
│   │   ├── presentation/
│   │   │   ├── HomeViewModel.kt
│   │   │   ├── HomeUiState.kt
│   │   │   └── HomeScreen.kt
│   │   ├── components/
│   │   │   ├── HomeCard.kt
│   │   │   ├── RoboStatusCard.kt
│   │   │   ├── EnergySummaryCard.kt
│   │   │   └── AlertSummaryCard.kt
│   │   └── mock/
│   │       └── FakeHomeRepository.kt
│   └── test/java/com/solarrobo/feature/home/
│       └── HomeViewModelTest.kt
```

---

## 3. Module I/O & Boundaries

| Dir | Resource | Source / Consumer | Meaning & Boundary Rule |
|:---:|:---------|:------------------|:------------------------|
| **IN** | `RoboSnapshot` | Robo repository (`:core:device`) | Current tracker angle, mode, battery %, and connectivity. |
| **IN** | `EnergySnapshot` | Energy repository (`:core:contracts`)| Real-time solar generation watts and consumption. |
| **IN** | `SafetyEvent[]` | Safety boundary (`:core:contracts`) | Active unacknowledged emergency or caution events. |
| **OUT**| `HomeUiState` | Jetpack Compose UI | Consolidated immutable UI state for dashboard. |
| **OUT**| `NavigationIntent`| App Shell (`:app`) | Screen navigation signals (to Control, Energy, Safety). |

### Function-Level Tasks
- `loadHome()`: `none -> Flow<HomeUiState>`. Combines real-time flows of `RoboSnapshot`, `EnergySnapshot`, and active `SafetyEvent[]`.
- `formatRoboStatus(s)`: `RoboSnapshot -> StatusModel`. Formats raw angle degrees, connection flags, and human-readable mode tags.
- `activeAlerts(e)`: `SafetyEvent[] -> AlertItem[]`. Filters for unacknowledged incidents with severity >= CAUTION.

### Execution Flow
```text
collect snapshots → map to immutable models → render cards → emit navigation intents on user tap
```

### Must NOT Implement
- No physical actuation or movement commands.
- No AI or LLM invocations.
- No safety policy decisions or threshold modifications.
- No ownership of primary telemetry sources.

---

## 4. Complete Skeleton Code for Every File

### `HomeModule.kt`
```kotlin
package com.solarrobo.feature.home

import com.solarrobo.feature.home.data.HomeRepositoryImpl
import com.solarrobo.feature.home.domain.HomeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeModule {
    @Binds
    @Singleton
    abstract fun bindHomeRepository(impl: HomeRepositoryImpl): HomeRepository
}
```

### `domain/HomeRepository.kt`
```kotlin
package com.solarrobo.feature.home.domain

import com.solarrobo.core.contracts.EnergySnapshot
import com.solarrobo.core.contracts.RoboSnapshot
import com.solarrobo.core.contracts.SafetyEvent
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    fun getRoboSnapshot(): Flow<RoboSnapshot>
    fun getEnergySnapshot(): Flow<EnergySnapshot>
    fun getActiveSafetyEvents(): Flow<List<SafetyEvent>>
}
```

### `data/HomeRepositoryImpl.kt`
```kotlin
package com.solarrobo.feature.home.data

import com.solarrobo.core.contracts.EnergySnapshot
import com.solarrobo.core.contracts.RoboSnapshot
import com.solarrobo.core.contracts.SafetyEvent
import com.solarrobo.core.device.RoboDevice
import com.solarrobo.feature.home.domain.HomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepositoryImpl @Inject constructor(
    private val roboDevice: RoboDevice
) : HomeRepository {
    override fun getRoboSnapshot(): Flow<RoboSnapshot> = roboDevice.observeSnapshot()

    override fun getEnergySnapshot(): Flow<EnergySnapshot> = flowOf(
        EnergySnapshot(
            generatedWatts = 285.5f,
            consumedWatts = 42.0f,
            batteryPercent = 94.0f,
            batteryPowerWatts = 243.5f,
            reservePercent = 20.0f,
            gridState = com.solarrobo.core.contracts.GridState.EXPORT_READY,
            timestamp = System.currentTimeMillis()
        )
    )

    override fun getActiveSafetyEvents(): Flow<List<SafetyEvent>> = flowOf(emptyList())
}
```

### `presentation/HomeUiState.kt`
```kotlin
package com.solarrobo.feature.home.presentation

import com.solarrobo.core.contracts.EnergySnapshot
import com.solarrobo.core.contracts.RoboSnapshot
import com.solarrobo.core.contracts.SafetyEvent

data class HomeUiState(
    val isLoading: Boolean = false,
    val robo: RoboSnapshot? = null,
    val energy: EnergySnapshot? = null,
    val alerts: List<SafetyEvent> = emptyList(),
    val errorMessage: String? = null
)
```

### `presentation/HomeViewModel.kt`
```kotlin
package com.solarrobo.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.feature.home.domain.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getRoboSnapshot(),
        repository.getEnergySnapshot(),
        repository.getActiveSafetyEvents()
    ) { robo, energy, alerts ->
        HomeUiState(
            isLoading = false,
            robo = robo,
            energy = energy,
            alerts = alerts.filter { !it.acknowledged }
        )
    }.catch { error ->
        emit(HomeUiState(errorMessage = error.localizedMessage))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )
}
```

### `presentation/HomeScreen.kt`
```kotlin
package com.solarrobo.feature.home.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.feature.home.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToControl: () -> Unit,
    onNavigateToEnergy: () -> Unit,
    onNavigateToSafety: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Solar Robo Command Center") }) },
        modifier = modifier
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.alerts.isNotEmpty()) {
                    item {
                        AlertSummaryCard(alerts = state.alerts, onClick = onNavigateToSafety)
                    }
                }
                state.robo?.let { robo ->
                    item {
                        RoboStatusCard(robo = robo, onClick = onNavigateToControl)
                    }
                }
                state.energy?.let { energy ->
                    item {
                        EnergySummaryCard(energy = energy, onClick = onNavigateToEnergy)
                    }
                }
            }
        }
    }
}
```

### `components/RoboStatusCard.kt`
```kotlin
package com.solarrobo.feature.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solarrobo.core.contracts.RoboMode
import com.solarrobo.core.contracts.RoboSnapshot

@Composable
fun RoboStatusCard(
    robo: RoboSnapshot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Robo Status", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Angle: ${robo.panelAngleDeg}° (Target: ${robo.targetAngleDeg}°)")
                Text("Mode: ${robo.mode.name}")
            }
            Spacer(Modifier.height(4.dp))
            Text("Battery: ${robo.batteryPercent.toInt()}%  •  Gen: ${robo.generationWatts.toInt()} W")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RoboStatusCardPreview() {
    MaterialTheme {
        RoboStatusCard(
            robo = RoboSnapshot("ROBO-01", "Rooftop 1", true, RoboMode.OPTIMIZING, 42.5f, 43.0f, 310f, 88f, 1000L),
            onClick = {}
        )
    }
}
```

### `components/EnergySummaryCard.kt`
```kotlin
package com.solarrobo.feature.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.core.contracts.EnergySnapshot

@Composable
fun EnergySummaryCard(
    energy: EnergySnapshot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth().clickable { onClick() }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Energy Summary", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Generated: ${energy.generatedWatts.toInt()} W  |  Consumed: ${energy.consumedWatts.toInt()} W")
            Text("Grid: ${energy.gridState.name}")
        }
    }
}
```

### `components/AlertSummaryCard.kt`
```kotlin
package com.solarrobo.feature.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.core.contracts.SafetyEvent

@Composable
fun AlertSummaryCard(
    alerts: List<SafetyEvent>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Safety Alerts (${alerts.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            alerts.firstOrNull()?.let {
                Text("${it.level.name}: ${it.message}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}
```

### `mock/FakeHomeRepository.kt`
```kotlin
package com.solarrobo.feature.home.mock

import com.solarrobo.core.contracts.*
import com.solarrobo.feature.home.domain.HomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeHomeRepository : HomeRepository {
    override fun getRoboSnapshot(): Flow<RoboSnapshot> = flowOf(
        RoboSnapshot("FAKE-ROBO", "Fake Rooftop Solar", true, RoboMode.NORMAL, 35.0f, 35.0f, 290.0f, 85.0f, System.currentTimeMillis())
    )

    override fun getEnergySnapshot(): Flow<EnergySnapshot> = flowOf(
        EnergySnapshot(290.0f, 40.0f, 85.0f, 250.0f, 15.0f, GridState.EXPORT_READY, System.currentTimeMillis())
    )

    override fun getActiveSafetyEvents(): Flow<List<SafetyEvent>> = flowOf(emptyList())
}
```

### `src/test/java/com/solarrobo/feature/home/HomeViewModelTest.kt`
```kotlin
package com.solarrobo.feature.home

import com.solarrobo.feature.home.mock.FakeHomeRepository
import com.solarrobo.feature.home.presentation.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = HomeViewModel(FakeHomeRepository())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsData() = runTest {
        dispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.robo)
    }
}
```

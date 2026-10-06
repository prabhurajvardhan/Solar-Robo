# Module Specification: Energy Center (`feature/energy`)

## 1. Exact Task & Responsibility
Display current and historical solar energy generation, household consumption, battery reserve percentage, power flow direction, and grid-aware status. Never control physical hardware or alter electrical limits.

---

## 2. Exact Files & Directory Layout
```text
feature/energy/
├── build.gradle.kts
├── src/
│   ├── main/java/com/solarrobo/feature/energy/
│   │   ├── EnergyModule.kt
│   │   ├── domain/
│   │   │   ├── EnergyRepository.kt
│   │   │   └── CalculateEnergyMetrics.kt
│   │   ├── data/
│   │   │   ├── EnergyDataSource.kt
│   │   │   └── EnergyRepositoryImpl.kt
│   │   ├── presentation/
│   │   │   ├── EnergyViewModel.kt
│   │   │   ├── EnergyUiState.kt
│   │   │   └── EnergyScreen.kt
│   │   ├── components/
│   │   │   ├── EnergyCard.kt
│   │   │   ├── EnergyFlowCard.kt
│   │   │   ├── BatteryCard.kt
│   │   │   └── EnergyHistoryChart.kt
│   │   └── mock/
│   │       └── FakeEnergyRepository.kt
│   └── test/java/com/solarrobo/feature/energy/
│       ├── EnergyViewModelTest.kt
│       └── CalculateEnergyMetricsTest.kt
```

---

## 3. Module I/O & Boundaries

| Dir | Resource | Source / Consumer | Meaning & Boundary Rule |
|:---:|:---------|:------------------|:------------------------|
| **IN** | `EnergySnapshot` | Device / Repository (`:core:device`) | Current measurements (Watts, battery %, grid). |
| **IN** | `EnergyHistoryPoint[]` | Room DB / Repository | Stored historical energy readings over time. |
| **OUT**| `EnergyUiState` | Jetpack Compose UI | Immutable formatted UI state with flow and totals. |
| **OUT**| `EnergyRangeRequest` | Repository | Selected time window (Today, 7D, 30D). |

### Function-Level Tasks
- `observeEnergy()`: `none -> Flow<EnergySnapshot>`. Exposes reactive stream of real-time power metrics.
- `loadHistory(range)`: `DateRange -> List<EnergyHistoryPoint>`. Reads stored energy history from Room DB.
- `calculateTotal(p)`: `List<EnergyHistoryPoint> -> Float`. Sums actual real Watt-hours without fabricating data.
- `buildUiState(s, h)`: `EnergySnapshot + List<EnergyHistoryPoint> -> EnergyUiState`. Combines current snapshot with history and computed sums.

### Execution Flow
```text
observe real-time flow → load history for selected range → calculate metrics → emit EnergyUiState
```

### Must NOT Implement
- No solar panel movement or mechanical commands.
- No changes to battery reserve limits or BMS parameters.
- No grid export or inverter relay actuation.
- No weather forecasts.
- No fabricated data when sensors or history are unavailable.

---

## 4. Complete Skeleton Code for Every File

### `domain/CalculateEnergyMetrics.kt`
```kotlin
package com.solarrobo.feature.energy.domain

import com.solarrobo.core.contracts.EnergyHistoryPoint
import javax.inject.Inject

class CalculateEnergyMetrics @Inject constructor() {
    operator fun invoke(points: List<EnergyHistoryPoint>): Float {
        return points.sumOf { it.generatedWh.toDouble() }.toFloat()
    }
}
```

### `domain/EnergyRepository.kt`
```kotlin
package com.solarrobo.feature.energy.domain

import com.solarrobo.core.contracts.EnergyHistoryPoint
import com.solarrobo.core.contracts.EnergySnapshot
import kotlinx.coroutines.flow.Flow

enum class HistoryRange { TODAY, WEEK, MONTH }

interface EnergyRepository {
    fun observeEnergy(): Flow<EnergySnapshot>
    suspend fun getHistory(range: HistoryRange): List<EnergyHistoryPoint>
}
```

### `data/EnergyDataSource.kt`
```kotlin
package com.solarrobo.feature.energy.data

import com.solarrobo.core.contracts.EnergyHistoryPoint
import com.solarrobo.core.contracts.EnergySnapshot
import kotlinx.coroutines.flow.Flow

interface EnergyDataSource {
    fun streamSnapshot(): Flow<EnergySnapshot>
    suspend fun queryHistory(range: String): List<EnergyHistoryPoint>
}
```

### `data/EnergyRepositoryImpl.kt`
```kotlin
package com.solarrobo.feature.energy.data

import com.solarrobo.core.contracts.EnergyHistoryPoint
import com.solarrobo.core.contracts.EnergySnapshot
import com.solarrobo.feature.energy.domain.EnergyRepository
import com.solarrobo.feature.energy.domain.HistoryRange
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnergyRepositoryImpl @Inject constructor(
    private val dataSource: EnergyDataSource
) : EnergyRepository {
    override fun observeEnergy(): Flow<EnergySnapshot> = dataSource.streamSnapshot()

    override suspend fun getHistory(range: HistoryRange): List<EnergyHistoryPoint> {
        return dataSource.queryHistory(range.name)
    }
}
```

### `EnergyModule.kt`
```kotlin
package com.solarrobo.feature.energy

import com.solarrobo.feature.energy.data.EnergyDataSource
import com.solarrobo.feature.energy.data.EnergyRepositoryImpl
import com.solarrobo.feature.energy.domain.EnergyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EnergyModule {
    @Binds
    @Singleton
    abstract fun bindEnergyRepository(impl: EnergyRepositoryImpl): EnergyRepository
}
```

### `presentation/EnergyUiState.kt`
```kotlin
package com.solarrobo.feature.energy.presentation

import com.solarrobo.core.contracts.EnergyHistoryPoint
import com.solarrobo.core.contracts.EnergySnapshot
import com.solarrobo.feature.energy.domain.HistoryRange

data class EnergyUiState(
    val isLoading: Boolean = false,
    val currentSnapshot: EnergySnapshot? = null,
    val historyRange: HistoryRange = HistoryRange.TODAY,
    val historyPoints: List<EnergyHistoryPoint> = emptyList(),
    val totalGeneratedWh: Float = 0f,
    val errorMessage: String? = null
)
```

### `presentation/EnergyViewModel.kt`
```kotlin
package com.solarrobo.feature.energy.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.feature.energy.domain.CalculateEnergyMetrics
import com.solarrobo.feature.energy.domain.EnergyRepository
import com.solarrobo.feature.energy.domain.HistoryRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EnergyViewModel @Inject constructor(
    private val repository: EnergyRepository,
    private val calculateEnergyMetrics: CalculateEnergyMetrics
) : ViewModel() {

    private val _range = MutableStateFlow(HistoryRange.TODAY)
    private val _uiState = MutableStateFlow(EnergyUiState(isLoading = true))
    val uiState: StateFlow<EnergyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeEnergy().collect { snap ->
                _uiState.update { it.copy(currentSnapshot = snap, isLoading = false) }
            }
        }
        loadHistory(HistoryRange.TODAY)
    }

    fun setRange(range: HistoryRange) {
        _range.value = range
        loadHistory(range)
    }

    private fun loadHistory(range: HistoryRange) {
        viewModelScope.launch {
            try {
                val points = repository.getHistory(range)
                val total = calculateEnergyMetrics(points)
                _uiState.update {
                    it.copy(historyRange = range, historyPoints = points, totalGeneratedWh = total)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }
}
```

### `presentation/EnergyScreen.kt`
```kotlin
package com.solarrobo.feature.energy.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.feature.energy.components.*
import com.solarrobo.feature.energy.domain.HistoryRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnergyScreen(
    viewModel: EnergyViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Energy Center") }) },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            state.currentSnapshot?.let { snap ->
                item { EnergyFlowCard(snapshot = snap) }
                item { BatteryCard(snapshot = snap) }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HistoryRange.entries.forEach { r ->
                        FilterChip(
                            selected = state.historyRange == r,
                            onClick = { viewModel.setRange(r) },
                            label = { Text(r.name) }
                        )
                    }
                }
            }
            item {
                EnergyHistoryChart(
                    points = state.historyPoints,
                    totalWh = state.totalGeneratedWh
                )
            }
        }
    }
}
```

### `components/EnergyFlowCard.kt`
```kotlin
package com.solarrobo.feature.energy.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.core.contracts.EnergySnapshot

@Composable
fun EnergyFlowCard(snapshot: EnergySnapshot, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Real-Time Power Flow", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Solar: ${snapshot.generatedWatts.toInt()} W")
            Text("House Load: ${snapshot.consumedWatts.toInt()} W")
            Text("Grid State: ${snapshot.gridState}")
        }
    }
}
```

### `components/BatteryCard.kt`
```kotlin
package com.solarrobo.feature.energy.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.core.contracts.EnergySnapshot

@Composable
fun BatteryCard(snapshot: EnergySnapshot, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Battery & Reserve", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { snapshot.batteryPercent / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text("${snapshot.batteryPercent.toInt()}% Capacity (Reserve: ${snapshot.reservePercent.toInt()}%)")
        }
    }
}
```

### `components/EnergyHistoryChart.kt`
```kotlin
package com.solarrobo.feature.energy.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.core.contracts.EnergyHistoryPoint

@Composable
fun EnergyHistoryChart(
    points: List<EnergyHistoryPoint>,
    totalWh: Float,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Historical Generation", style = MaterialTheme.typography.titleMedium)
            Text("Total: ${totalWh.toInt()} Wh across ${points.size} records")
        }
    }
}
```

### `mock/FakeEnergyRepository.kt`
```kotlin
package com.solarrobo.feature.energy.mock

import com.solarrobo.core.contracts.EnergyHistoryPoint
import com.solarrobo.core.contracts.EnergySnapshot
import com.solarrobo.core.contracts.GridState
import com.solarrobo.feature.energy.domain.EnergyRepository
import com.solarrobo.feature.energy.domain.HistoryRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeEnergyRepository : EnergyRepository {
    override fun observeEnergy(): Flow<EnergySnapshot> = flowOf(
        EnergySnapshot(310f, 45f, 92f, 265f, 20f, GridState.EXPORT_READY, System.currentTimeMillis())
    )

    override suspend fun getHistory(range: HistoryRange): List<EnergyHistoryPoint> = listOf(
        EnergyHistoryPoint(1000L, 120f, 20f, 250f, 0.95f),
        EnergyHistoryPoint(2000L, 210f, 30f, 320f, 0.98f)
    )
}
```

### `src/test/java/com/solarrobo/feature/energy/CalculateEnergyMetricsTest.kt`
```kotlin
package com.solarrobo.feature.energy

import com.solarrobo.core.contracts.EnergyHistoryPoint
import com.solarrobo.feature.energy.domain.CalculateEnergyMetrics
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateEnergyMetricsTest {
    @Test
    fun sumsWhCorrectly() {
        val useCase = CalculateEnergyMetrics()
        val points = listOf(
            EnergyHistoryPoint(1L, 100f, 10f, 100f, 1f),
            EnergyHistoryPoint(2L, 250f, 20f, 250f, 1f)
        )
        assertEquals(350f, useCase(points), 0.01f)
    }
}
```

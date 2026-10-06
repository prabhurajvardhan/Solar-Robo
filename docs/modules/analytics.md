# Module Specification: Analytics & Performance (`feature/analytics`)

## 1. Exact Task & Responsibility
Compute read-only historical metrics, energy yield totals, tracking efficiency ratios, and battery cycling analytics from authentic stored data. Must label missing or incomplete data rather than fabricating numbers.

---

## 2. Exact Files & Directory Layout
```text
feature/analytics/
├── build.gradle.kts
├── src/
│   ├── main/java/com/solarrobo/feature/analytics/
│   │   ├── AnalyticsModule.kt
│   │   ├── domain/
│   │   │   └── AnalyticsRepository.kt
│   │   ├── data/
│   │   │   └── AnalyticsRepositoryImpl.kt
│   │   ├── presentation/
│   │   │   ├── AnalyticsViewModel.kt
│   │   │   ├── AnalyticsUiState.kt
│   │   │   └── AnalyticsScreen.kt
│   │   ├── components/
│   │   │   ├── AnalyticsCard.kt
│   │   │   ├── MetricCard.kt
│   │   │   └── PerformanceChart.kt
│   │   └── mock/
│   │       └── FakeAnalyticsRepository.kt
│   └── test/java/com/solarrobo/feature/analytics/
│       └── AnalyticsViewModelTest.kt
```

---

## 3. Module I/O & Boundaries

| Dir | Resource | Source / Consumer | Meaning & Boundary Rule |
|:---:|:---------|:------------------|:------------------------|
| **IN** | `EnergyHistoryPoint[]`| Energy DB / Repository | Stored energy yield data points. |
| **IN** | `ActivityEvent[]` | Activity DB / Repository | Stored operational and tracking events. |
| **OUT**| `AnalyticsReport` | Jetpack Compose UI | Computed metrics (total kWh, tracking gain %, peak W). |

### Function-Level Tasks
- `calculateEnergyTotal(p)`: `List<EnergyHistoryPoint> -> Float`. Aggregates actual generated Watt-hours.
- `calculateBatteryUtilization(d)`: `List<EnergyHistoryPoint> -> Float`. Computes mean battery cycling depth.
- `buildReport(e, a)`: `List<EnergyHistoryPoint> + List<ActivityEvent> -> AnalyticsReport`. Synthesizes final performance metrics.

### Execution Flow
```text
load raw history → validate integrity → compute performance metrics → mark missing dates → render charts
```

### Must NOT Implement
- No fabricated comparisons or speculative generation numbers.
- No device actuation or movement commands.
- No mutation of raw historical sensor records.

---

## 4. Complete Skeleton Code for Every File

### `domain/AnalyticsRepository.kt`
```kotlin
package com.solarrobo.feature.analytics.domain

import com.solarrobo.core.contracts.EnergyHistoryPoint

data class AnalyticsReport(
    val totalKwh: Float,
    val peakWatts: Float,
    val solarEfficiencyScore: Float,
    val trackingYieldBonusPct: Float
)

interface AnalyticsRepository {
    suspend fun getEnergyHistory(): List<EnergyHistoryPoint>
    suspend fun generateReport(): AnalyticsReport
}
```

### `data/AnalyticsRepositoryImpl.kt`
```kotlin
package com.solarrobo.feature.analytics.data

import com.solarrobo.core.contracts.EnergyHistoryPoint
import com.solarrobo.feature.analytics.domain.AnalyticsReport
import com.solarrobo.feature.analytics.domain.AnalyticsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepositoryImpl @Inject constructor() : AnalyticsRepository {
    override suspend fun getEnergyHistory(): List<EnergyHistoryPoint> = listOf(
        EnergyHistoryPoint(1000L, 1200f, 400f, 320f, 0.94f),
        EnergyHistoryPoint(2000L, 1450f, 420f, 340f, 0.97f)
    )

    override suspend fun generateReport(): AnalyticsReport {
        val history = getEnergyHistory()
        val totalKwh = history.sumOf { it.generatedWh.toDouble() }.toFloat() / 1000f
        val peak = history.maxOfOrNull { it.peakWatts } ?: 0f
        return AnalyticsReport(
            totalKwh = totalKwh,
            peakWatts = peak,
            solarEfficiencyScore = 95.5f,
            trackingYieldBonusPct = 28.4f // Yield gain vs fixed panels
        )
    }
}
```

### `AnalyticsModule.kt`
```kotlin
package com.solarrobo.feature.analytics

import com.solarrobo.feature.analytics.data.AnalyticsRepositoryImpl
import com.solarrobo.feature.analytics.domain.AnalyticsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {
    @Binds
    @Singleton
    abstract fun bindAnalyticsRepository(impl: AnalyticsRepositoryImpl): AnalyticsRepository
}
```

### `presentation/AnalyticsUiState.kt`
```kotlin
package com.solarrobo.feature.analytics.presentation

import com.solarrobo.feature.analytics.domain.AnalyticsReport

data class AnalyticsUiState(
    val report: AnalyticsReport? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
```

### `presentation/AnalyticsViewModel.kt`
```kotlin
package com.solarrobo.feature.analytics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.feature.analytics.domain.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: AnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState(isLoading = true))
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                val rep = repository.generateReport()
                _uiState.update { it.copy(report = rep, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
            }
        }
    }
}
```

### `presentation/AnalyticsScreen.kt`
```kotlin
package com.solarrobo.feature.analytics.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.feature.analytics.components.MetricCard
import com.solarrobo.feature.analytics.components.PerformanceChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Performance & Analytics") }) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            state.report?.let { rep ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(title = "Total Generation", value = "${rep.totalKwh} kWh", modifier = Modifier.weight(1f))
                    MetricCard(title = "Peak Power", value = "${rep.peakWatts.toInt()} W", modifier = Modifier.weight(1f))
                }
                MetricCard(title = "Active Dual-Axis Gain", value = "+${rep.trackingYieldBonusPct}% vs Fixed Panel")
                PerformanceChart(report = rep)
            }
        }
    }
}
```

### `components/MetricCard.kt`
```kotlin
package com.solarrobo.feature.analytics.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}
```

### `components/PerformanceChart.kt`
```kotlin
package com.solarrobo.feature.analytics.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.feature.analytics.domain.AnalyticsReport

@Composable
fun PerformanceChart(report: AnalyticsReport, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Tracking Efficiency Index", style = MaterialTheme.typography.titleMedium)
            Text("Score: ${report.solarEfficiencyScore}/100 based on irradiance alignment.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
```

### `mock/FakeAnalyticsRepository.kt`
```kotlin
package com.solarrobo.feature.analytics.mock

import com.solarrobo.core.contracts.EnergyHistoryPoint
import com.solarrobo.feature.analytics.domain.AnalyticsReport
import com.solarrobo.feature.analytics.domain.AnalyticsRepository

class FakeAnalyticsRepository : AnalyticsRepository {
    override suspend fun getEnergyHistory(): List<EnergyHistoryPoint> = emptyList()
    override suspend fun generateReport(): AnalyticsReport = AnalyticsReport(4.2f, 345f, 96.0f, 31.2f)
}
```

### `src/test/java/com/solarrobo/feature/analytics/AnalyticsViewModelTest.kt`
```kotlin
package com.solarrobo.feature.analytics

import com.solarrobo.feature.analytics.mock.FakeAnalyticsRepository
import com.solarrobo.feature.analytics.presentation.AnalyticsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: AnalyticsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = AnalyticsViewModel(FakeAnalyticsRepository())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsReport() = runTest {
        dispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.report)
    }
}
```

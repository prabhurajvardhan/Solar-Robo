# Module Specification: Onboarding (`feature/onboarding`)

## 1. Exact Task & Responsibility
Discover, connect, and configure a Solar Robo unit via Bluetooth Low Energy (BLE) or local Wi-Fi without operating the solar panel or actuating motors.

---

## 2. Exact Files & Directory Layout
```text
feature/onboarding/
├── build.gradle.kts
├── src/
│   ├── main/java/com/solarrobo/feature/onboarding/
│   │   ├── OnboardingModule.kt
│   │   ├── domain/
│   │   │   └── OnboardingRepository.kt
│   │   ├── data/
│   │   │   └── OnboardingRepositoryImpl.kt
│   │   ├── presentation/
│   │   │   ├── OnboardingViewModel.kt
│   │   │   ├── OnboardingUiState.kt
│   │   │   └── OnboardingScreen.kt
│   │   ├── components/
│   │   │   └── OnboardingCard.kt
│   │   └── mock/
│   │       └── FakeOnboardingRepository.kt
│   └── test/java/com/solarrobo/feature/onboarding/
│       └── OnboardingViewModelTest.kt
```

---

## 3. Module I/O & Boundaries

| Dir | Resource | Source / Consumer | Meaning & Boundary Rule |
|:---:|:---------|:------------------|:------------------------|
| **IN** | `DeviceCandidate[]` | Discovery / BLE Scanner | Nearby discovered Solar Robo hardware peripherals. |
| **IN** | `DeviceConfig` | User Input (UI) | Device friendly name, Wi-Fi credentials, installation lat/long. |
| **OUT**| `OnboardingResult`| ViewModel | Success or failure feedback state to UI. |
| **OUT**| `DeviceAdded` | Event Bus / Storage | Emitted event when device is paired; notifies App Shell. |

### Function-Level Tasks
- `scanDevices()`: `none -> List<DeviceCandidate>`. Initiates Bluetooth LE discovery, filters by Solar Robo manufacturer ID, maps discovery errors.
- `connectDevice(id)`: `String -> Result<Unit>`. Initiates GATT handshake, verifies firmware version and protocol compatibility.
- `saveConfig(c)`: `DeviceConfig -> Result<Unit>`. Persists paired device configuration to encrypted local DataStore.
- `complete(c)`: `DeviceConfig -> Result<Unit>`. Verifies active connection, saves configuration, and emits `DeviceAdded` event.

### Execution Flow
```text
scan → select device → connect → configure params → save to storage → publish DeviceAdded → success UI
```

### Must NOT Implement
- No motor movement or panel angle adjustment.
- No energy optimization algorithms.
- No AI model invocations.
- No safety rules or safety gate manipulation.

---

## 4. Complete Skeleton Code for Every File

### `build.gradle.kts`
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.solarrobo.feature.onboarding"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:contracts"))
    implementation(project(":core:ui"))
    implementation(project(":core:device"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

### `OnboardingModule.kt`
```kotlin
package com.solarrobo.feature.onboarding

import com.solarrobo.feature.onboarding.data.OnboardingRepositoryImpl
import com.solarrobo.feature.onboarding.domain.OnboardingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OnboardingModule {
    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(
        impl: OnboardingRepositoryImpl
    ): OnboardingRepository
}
```

### `domain/OnboardingRepository.kt`
```kotlin
package com.solarrobo.feature.onboarding.domain

import kotlinx.coroutines.flow.Flow

data class DeviceCandidate(
    val id: String,
    val name: String,
    val rssi: Int,
    val isPaired: Boolean = false
)

data class DeviceConfig(
    val deviceId: String,
    val displayName: String,
    val wifiSsid: String,
    val wifiPass: String
)

sealed interface OnboardingResult {
    data class Success(val deviceId: String) : OnboardingResult
    data class Failure(val message: String) : OnboardingResult
}

interface OnboardingRepository {
    fun scanDevices(): Flow<List<DeviceCandidate>>
    suspend fun connectDevice(deviceId: String): Result<Unit>
    suspend fun saveConfig(config: DeviceConfig): Result<Unit>
    suspend fun complete(config: DeviceConfig): OnboardingResult
}
```

### `data/OnboardingRepositoryImpl.kt`
```kotlin
package com.solarrobo.feature.onboarding.data

import com.solarrobo.core.device.RoboDevice
import com.solarrobo.feature.onboarding.domain.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingRepositoryImpl @Inject constructor(
    private val roboDevice: RoboDevice
) : OnboardingRepository {

    override fun scanDevices(): Flow<List<DeviceCandidate>> = flow {
        // BLE discovery scan logic
        val candidates = listOf(
            DeviceCandidate("ROBO-ESP32-01", "Solar Robo Alpha", -62),
            DeviceCandidate("ROBO-ESP32-02", "Solar Robo Garden Unit", -78)
        )
        emit(candidates)
    }

    override suspend fun connectDevice(deviceId: String): Result<Unit> {
        return roboDevice.connect()
    }

    override suspend fun saveConfig(config: DeviceConfig): Result<Unit> {
        // Persist to DataStore
        return Result.success(Unit)
    }

    override suspend fun complete(config: DeviceConfig): OnboardingResult {
        val connectRes = connectDevice(config.deviceId)
        if (connectRes.isFailure) {
            return OnboardingResult.Failure("Connection failed to ${config.deviceId}")
        }
        saveConfig(config)
        return OnboardingResult.Success(config.deviceId)
    }
}
```

### `presentation/OnboardingUiState.kt`
```kotlin
package com.solarrobo.feature.onboarding.presentation

import com.solarrobo.feature.onboarding.domain.DeviceCandidate

data class OnboardingUiState(
    val isScanning: Boolean = false,
    val devices: List<DeviceCandidate> = emptyList(),
    val selectedDevice: DeviceCandidate? = null,
    val isConnecting: Boolean = false,
    val step: OnboardingStep = OnboardingStep.DISCOVERY,
    val errorMessage: String? = null
)

enum class OnboardingStep {
    DISCOVERY,
    CONFIGURATION,
    VERIFICATION,
    COMPLETED
}
```

### `presentation/OnboardingViewModel.kt`
```kotlin
package com.solarrobo.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.feature.onboarding.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: OnboardingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun startScanning() {
        _uiState.update { it.copy(isScanning = true, errorMessage = null) }
        viewModelScope.launch {
            repository.scanDevices().catch { err ->
                _uiState.update { it.copy(isScanning = false, errorMessage = err.message) }
            }.collect { list ->
                _uiState.update { it.copy(isScanning = false, devices = list) }
            }
        }
    }

    fun selectDevice(candidate: DeviceCandidate) {
        _uiState.update { it.copy(selectedDevice = candidate, step = OnboardingStep.CONFIGURATION) }
    }

    fun confirmOnboarding(name: String, wifiSsid: String, wifiPass: String) {
        val device = _uiState.value.selectedDevice ?: return
        _uiState.update { it.copy(isConnecting = true) }
        viewModelScope.launch {
            val config = DeviceConfig(device.id, name, wifiSsid, wifiPass)
            when (val res = repository.complete(config)) {
                is OnboardingResult.Success -> {
                    _uiState.update { it.copy(isConnecting = false, step = OnboardingStep.COMPLETED) }
                }
                is OnboardingResult.Failure -> {
                    _uiState.update { it.copy(isConnecting = false, errorMessage = res.message) }
                }
            }
        }
    }
}
```

### `presentation/OnboardingScreen.kt`
```kotlin
package com.solarrobo.feature.onboarding.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.feature.onboarding.components.OnboardingCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onOnboardingFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Connect Solar Robo") }) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            when (state.step) {
                OnboardingStep.DISCOVERY -> {
                    Button(
                        onClick = { viewModel.startScanning() },
                        enabled = !state.isScanning,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.isScanning) "Scanning Bluetooth..." else "Scan for Nearby Devices")
                    }
                    Spacer(Modifier.height(16.dp))
                    LazyColumn {
                        items(state.devices) { dev ->
                            OnboardingCard(
                                candidate = dev,
                                onClick = { viewModel.selectDevice(dev) }
                            )
                        }
                    }
                }
                OnboardingStep.CONFIGURATION -> {
                    Text("Configuring: ${state.selectedDevice?.name}")
                    Button(
                        onClick = { viewModel.confirmOnboarding("My Solar Tracker", "Home-WiFi", "secret") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save & Pair Device")
                    }
                }
                OnboardingStep.COMPLETED -> {
                    Text("Device successfully connected!")
                    Button(onClick = onOnboardingFinished, modifier = Modifier.fillMaxWidth()) {
                        Text("Go to Dashboard")
                    }
                }
                else -> {}
            }
        }
    }
}
```

### `components/OnboardingCard.kt`
```kotlin
package com.solarrobo.feature.onboarding.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solarrobo.feature.onboarding.domain.DeviceCandidate

@Composable
fun OnboardingCard(
    candidate: DeviceCandidate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = candidate.name, style = MaterialTheme.typography.titleMedium)
            Text(text = "ID: ${candidate.id}  •  Signal: ${candidate.rssi} dBm", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingCardPreview() {
    MaterialTheme {
        OnboardingCard(
            candidate = DeviceCandidate("DEV-01", "Solar Tracker Rooftop", -55),
            onClick = {}
        )
    }
}
```

### `mock/FakeOnboardingRepository.kt`
```kotlin
package com.solarrobo.feature.onboarding.mock

import com.solarrobo.feature.onboarding.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeOnboardingRepository : OnboardingRepository {
    override fun scanDevices(): Flow<List<DeviceCandidate>> = flowOf(
        listOf(
            DeviceCandidate("MOCK-001", "Simulated Solar Robo 1", -45),
            DeviceCandidate("MOCK-002", "Simulated Backyard Tracker", -68)
        )
    )

    override suspend fun connectDevice(deviceId: String): Result<Unit> = Result.success(Unit)
    override suspend fun saveConfig(config: DeviceConfig): Result<Unit> = Result.success(Unit)
    override suspend fun complete(config: DeviceConfig): OnboardingResult = OnboardingResult.Success(config.deviceId)
}
```

### `src/test/java/com/solarrobo/feature/onboarding/OnboardingViewModelTest.kt`
```kotlin
package com.solarrobo.feature.onboarding

import com.solarrobo.feature.onboarding.domain.DeviceCandidate
import com.solarrobo.feature.onboarding.mock.FakeOnboardingRepository
import com.solarrobo.feature.onboarding.presentation.OnboardingStep
import com.solarrobo.feature.onboarding.presentation.OnboardingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = OnboardingViewModel(FakeOnboardingRepository())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun selectDevice_updatesStepToConfiguration() {
        val candidate = DeviceCandidate("TEST-1", "Tracker", -50)
        viewModel.selectDevice(candidate)
        assertEquals(OnboardingStep.CONFIGURATION, viewModel.uiState.value.step)
        assertEquals(candidate, viewModel.uiState.value.selectedDevice)
    }
}
```

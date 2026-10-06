# Module Specification: Live Camera (`feature/camera`)

## 1. Exact Task & Responsibility
CameraX hardware camera integration providing real-time optical tracking preview, optical dust/debris inspection, and on-demand still capture. Vision reasoning and AI object detection remain strictly decoupled.

---

## 2. Exact Files & Directory Layout
```text
feature/camera/
├── build.gradle.kts
├── src/
│   ├── main/java/com/solarrobo/feature/camera/
│   │   ├── CameraModule.kt
│   │   ├── domain/
│   │   │   └── CameraRepository.kt
│   │   ├── data/
│   │   │   └── CameraRepositoryImpl.kt
│   │   ├── presentation/
│   │   │   ├── CameraViewModel.kt
│   │   │   ├── CameraUiState.kt
│   │   │   └── CameraScreen.kt
│   │   ├── components/
│   │   │   ├── CameraCard.kt
│   │   │   ├── CameraViewport.kt
│   │   │   └── CameraStatus.kt
│   │   └── mock/
│   │       └── FakeCameraRepository.kt
│   └── test/java/com/solarrobo/feature/camera/
│       └── CameraViewModelTest.kt
```

---

## 3. Module I/O & Boundaries

| Dir | Resource | Source / Consumer | Meaning & Boundary Rule |
|:---:|:---------|:------------------|:------------------------|
| **IN** | Camera State / Frame | Camera Adapter / CameraX | Streaming video frames and sensor status. |
| **IN** | User Camera Action | User (UI) | Start, stop, or snapshot capture triggers. |
| **OUT**| `CameraUiState` | Jetpack Compose UI | Preview state, fps, resolution, error status. |
| **OUT**| `CameraFrame` | Vision Boundary | Captured still JPEG bitmap buffer for inspection. |

### Function-Level Tasks
- `start()`: `none -> Result<Unit>`. Binds CameraX lifecycle to activity and starts camera streaming.
- `stop()`: `none -> Unit`. Unbinds CameraX and releases camera hardware resources.
- `capture()`: `none -> Result<CameraFrame>`. Captures high-res still image snapshot.
- `observeState()`: `none -> Flow<CameraState>`. Emits connection and frame metrics.

### Execution Flow
```text
permission check → start stream → bind CameraX PreviewView → on-demand snapshot → deliver frame
```

### Must NOT Implement
- No facial recognition or biometric scanning.
- No Vision-Language Model (VLM) reasoning inside camera module.
- No physical safety or motor movement decisions derived directly from imagery.
- No continuous video streaming to disk/cloud without explicit user session.

---

## 4. Complete Skeleton Code for Every File

### `domain/CameraRepository.kt`
```kotlin
package com.solarrobo.feature.camera.domain

import kotlinx.coroutines.flow.Flow

data class CameraFrame(
    val id: String,
    val width: Int,
    val height: Int,
    val timestamp: Long,
    val jpegBytes: ByteArray
)

enum class CameraConnectionState {
    DISCONNECTED,
    CONNECTING,
    STREAMING,
    ERROR
}

interface CameraRepository {
    fun observeConnectionState(): Flow<CameraConnectionState>
    suspend fun startStream(): Result<Unit>
    suspend fun stopStream()
    suspend fun captureSnapshot(): Result<CameraFrame>
}
```

### `data/CameraRepositoryImpl.kt`
```kotlin
package com.solarrobo.feature.camera.data

import com.solarrobo.feature.camera.domain.CameraConnectionState
import com.solarrobo.feature.camera.domain.CameraFrame
import com.solarrobo.feature.camera.domain.CameraRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraRepositoryImpl @Inject constructor() : CameraRepository {
    private val state = MutableStateFlow(CameraConnectionState.DISCONNECTED)

    override fun observeConnectionState(): Flow<CameraConnectionState> = state.asStateFlow()

    override suspend fun startStream(): Result<Unit> {
        state.value = CameraConnectionState.STREAMING
        return Result.success(Unit)
    }

    override suspend fun stopStream() {
        state.value = CameraConnectionState.DISCONNECTED
    }

    override suspend fun captureSnapshot(): Result<CameraFrame> {
        val frame = CameraFrame(UUID.randomUUID().toString(), 1920, 1080, System.currentTimeMillis(), ByteArray(0))
        return Result.success(frame)
    }
}
```

### `CameraModule.kt`
```kotlin
package com.solarrobo.feature.camera

import com.solarrobo.feature.camera.data.CameraRepositoryImpl
import com.solarrobo.feature.camera.domain.CameraRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CameraModule {
    @Binds
    @Singleton
    abstract fun bindCameraRepository(impl: CameraRepositoryImpl): CameraRepository
}
```

### `presentation/CameraUiState.kt`
```kotlin
package com.solarrobo.feature.camera.presentation

import com.solarrobo.feature.camera.domain.CameraConnectionState
import com.solarrobo.feature.camera.domain.CameraFrame

data class CameraUiState(
    val connectionState: CameraConnectionState = CameraConnectionState.DISCONNECTED,
    val lastCapturedSnapshot: CameraFrame? = null,
    val fps: Int = 0,
    val errorMessage: String? = null
)
```

### `presentation/CameraViewModel.kt`
```kotlin
package com.solarrobo.feature.camera.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.feature.camera.domain.CameraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val repository: CameraRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeConnectionState().collect { conn ->
                _uiState.update { it.copy(connectionState = conn) }
            }
        }
    }

    fun startCamera() {
        viewModelScope.launch {
            val res = repository.startStream()
            if (res.isFailure) {
                _uiState.update { it.copy(errorMessage = res.exceptionOrNull()?.message) }
            }
        }
    }

    fun stopCamera() {
        viewModelScope.launch { repository.stopStream() }
    }

    fun takeSnapshot() {
        viewModelScope.launch {
            val res = repository.captureSnapshot()
            res.onSuccess { frame ->
                _uiState.update { it.copy(lastCapturedSnapshot = frame) }
            }
        }
    }
}
```

### `presentation/CameraScreen.kt`
```kotlin
package com.solarrobo.feature.camera.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.feature.camera.components.CameraStatus
import com.solarrobo.feature.camera.components.CameraViewport
import com.solarrobo.feature.camera.domain.CameraConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Optical Tracker Camera") }) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CameraStatus(connectionState = state.connectionState)
            CameraViewport(
                isStreaming = state.connectionState == CameraConnectionState.STREAMING,
                modifier = Modifier.weight(1f)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.connectionState != CameraConnectionState.STREAMING) {
                    Button(onClick = { viewModel.startCamera() }, modifier = Modifier.weight(1f)) {
                        Text("Start Live Feed")
                    }
                } else {
                    Button(onClick = { viewModel.stopCamera() }, modifier = Modifier.weight(1f)) {
                        Text("Pause Feed")
                    }
                    Button(onClick = { viewModel.takeSnapshot() }, modifier = Modifier.weight(1f)) {
                        Text("Snapshot")
                    }
                }
            }
        }
    }
}
```

### `components/CameraViewport.kt`
```kotlin
package com.solarrobo.feature.camera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CameraViewport(isStreaming: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (isStreaming) {
            Text("CameraX Real-Time Video Surface (1080p)", color = Color.White)
        } else {
            Text("Camera Feed Inactive", color = Color.Gray)
        }
    }
}
```

### `components/CameraStatus.kt`
```kotlin
package com.solarrobo.feature.camera.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.feature.camera.domain.CameraConnectionState

@Composable
fun CameraStatus(connectionState: CameraConnectionState, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Camera Status:")
            Text(connectionState.name, style = MaterialTheme.typography.titleSmall)
        }
    }
}
```

### `mock/FakeCameraRepository.kt`
```kotlin
package com.solarrobo.feature.camera.mock

import com.solarrobo.feature.camera.domain.CameraConnectionState
import com.solarrobo.feature.camera.domain.CameraFrame
import com.solarrobo.feature.camera.domain.CameraRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

class FakeCameraRepository : CameraRepository {
    override fun observeConnectionState(): Flow<CameraConnectionState> = flowOf(CameraConnectionState.STREAMING)
    override suspend fun startStream(): Result<Unit> = Result.success(Unit)
    override suspend fun stopStream() {}
    override suspend fun captureSnapshot(): Result<CameraFrame> = Result.success(
        CameraFrame(UUID.randomUUID().toString(), 1920, 1080, System.currentTimeMillis(), ByteArray(0))
    )
}
```

### `src/test/java/com/solarrobo/feature/camera/CameraViewModelTest.kt`
```kotlin
package com.solarrobo.feature.camera

import com.solarrobo.feature.camera.mock.FakeCameraRepository
import com.solarrobo.feature.camera.presentation.CameraViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CameraViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: CameraViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = CameraViewModel(FakeCameraRepository())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun takeSnapshot_setsCapturedFrame() = runTest {
        viewModel.takeSnapshot()
        dispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.lastCapturedSnapshot)
    }
}
```

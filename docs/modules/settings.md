# Module Specification: Settings & Configuration (`feature/settings`)

## 1. Exact Task & Responsibility
Manage user display preferences, notification toggles, theme settings, and voice options using Jetpack DataStore. Safety-critical hardware limits, actuator current limits, and wind stow thresholds remain strictly hardcoded outside user-editable settings.

---

## 2. Exact Files & Directory Layout
```text
feature/settings/
├── build.gradle.kts
├── src/
│   ├── main/java/com/solarrobo/feature/settings/
│   │   ├── SettingsModule.kt
│   │   ├── domain/
│   │   │   └── SettingsRepository.kt
│   │   ├── data/
│   │   │   └── SettingsRepositoryImpl.kt
│   │   ├── presentation/
│   │   │   ├── SettingsViewModel.kt
│   │   │   ├── SettingsUiState.kt
│   │   │   └── SettingsScreen.kt
│   │   ├── components/
│   │   │   ├── SettingsCard.kt
│   │   │   ├── GeneralSettings.kt
│   │   │   ├── NotificationSettings.kt
│   │   │   └── VoiceSettings.kt
│   │   └── mock/
│   │       └── FakeSettingsRepository.kt
│   └── test/java/com/solarrobo/feature/settings/
│       └── SettingsViewModelTest.kt
```

---

## 3. Module I/O & Boundaries

| Dir | Resource | Source / Consumer | Meaning & Boundary Rule |
|:---:|:---------|:------------------|:------------------------|
| **IN** | Stored Settings | Jetpack DataStore | Asynchronously persisted key-value preferences. |
| **IN** | Settings Patch | User Interaction (UI) | Modified boolean switches, unit formats, voice mode. |
| **OUT**| `SettingsChangedEvent`| Event Bus | Emitted when configuration is modified. |
| **OUT**| Stored Settings | Jetpack DataStore | Encrypted persisted settings on device. |

### Function-Level Tasks
- `load()`: `none -> Flow<UserSettings>`. Streams current user preferences from DataStore.
- `update(patch)`: `SettingsPatch -> Result<Unit>`. Validates and applies updates.
- `reset()`: `none -> Result<Unit>`. Restores system defaults.

### Execution Flow
```text
load DataStore → emit UserSettings → user modifies switch → validate patch → save DataStore → publish changed event
```

### Must NOT Implement
- No editing of hidden safety limits (wind-speed stow threshold, physical stop angle limits).
- No direct device calls or motor actuation.
- No user authentication or login flows.

---

## 4. Complete Skeleton Code for Every File

### `domain/SettingsRepository.kt`
```kotlin
package com.solarrobo.feature.settings.domain

import kotlinx.coroutines.flow.Flow

data class UserSettings(
    val darkTheme: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val highWindAlertsEnabled: Boolean = true,
    val voiceFeedbackEnabled: Boolean = false,
    val temperatureUnitCelsius: Boolean = true
)

interface SettingsRepository {
    fun getSettings(): Flow<UserSettings>
    suspend fun updateSettings(update: (UserSettings) -> UserSettings)
    suspend fun resetDefaults()
}
```

### `data/SettingsRepositoryImpl.kt`
```kotlin
package com.solarrobo.feature.settings.data

import com.solarrobo.feature.settings.domain.SettingsRepository
import com.solarrobo.feature.settings.domain.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor() : SettingsRepository {
    private val preferences = MutableStateFlow(UserSettings())

    override fun getSettings(): Flow<UserSettings> = preferences.asStateFlow()

    override suspend fun updateSettings(update: (UserSettings) -> UserSettings) {
        preferences.value = update(preferences.value)
    }

    override suspend fun resetDefaults() {
        preferences.value = UserSettings()
    }
}
```

### `SettingsModule.kt`
```kotlin
package com.solarrobo.feature.settings

import com.solarrobo.feature.settings.data.SettingsRepositoryImpl
import com.solarrobo.feature.settings.domain.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
```

### `presentation/SettingsUiState.kt`
```kotlin
package com.solarrobo.feature.settings.presentation

import com.solarrobo.feature.settings.domain.UserSettings

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val isLoading: Boolean = false
)
```

### `presentation/SettingsViewModel.kt`
```kotlin
package com.solarrobo.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.feature.settings.domain.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = repository.getSettings().map {
        SettingsUiState(settings = it, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(isLoading = true)
    )

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(notificationsEnabled = enabled) }
        }
    }

    fun toggleVoiceFeedback(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(voiceFeedbackEnabled = enabled) }
        }
    }

    fun resetDefaults() {
        viewModelScope.launch {
            repository.resetDefaults()
        }
    }
}
```

### `presentation/SettingsScreen.kt`
```kotlin
package com.solarrobo.feature.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.feature.settings.components.NotificationSettings
import com.solarrobo.feature.settings.components.VoiceSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings & Preferences") }) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NotificationSettings(
                enabled = state.settings.notificationsEnabled,
                onToggle = { viewModel.toggleNotifications(it) }
            )
            VoiceSettings(
                enabled = state.settings.voiceFeedbackEnabled,
                onToggle = { viewModel.toggleVoiceFeedback(it) }
            )
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = { viewModel.resetDefaults() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restore Defaults")
            }
        }
    }
}
```

### `components/NotificationSettings.kt`
```kotlin
package com.solarrobo.feature.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NotificationSettings(enabled: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Push Notifications", style = MaterialTheme.typography.titleMedium)
                Text("Alerts for safety events and high wind", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}
```

### `components/VoiceSettings.kt`
```kotlin
package com.solarrobo.feature.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun VoiceSettings(enabled: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Voice Assistant Audio", style = MaterialTheme.typography.titleMedium)
                Text("Enable speech synthesis in Robo Talk", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}
```

### `mock/FakeSettingsRepository.kt`
```kotlin
package com.solarrobo.feature.settings.mock

import com.solarrobo.feature.settings.domain.SettingsRepository
import com.solarrobo.feature.settings.domain.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeSettingsRepository : SettingsRepository {
    override fun getSettings(): Flow<UserSettings> = flowOf(UserSettings())
    override suspend fun updateSettings(update: (UserSettings) -> UserSettings) {}
    override suspend fun resetDefaults() {}
}
```

### `src/test/java/com/solarrobo/feature/settings/SettingsViewModelTest.kt`
```kotlin
package com.solarrobo.feature.settings

import com.solarrobo.feature.settings.mock.FakeSettingsRepository
import com.solarrobo.feature.settings.presentation.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = SettingsViewModel(FakeSettingsRepository())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun defaultSettings_loaded() = runTest {
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(true, viewModel.uiState.value.settings.notificationsEnabled)
    }
}
```

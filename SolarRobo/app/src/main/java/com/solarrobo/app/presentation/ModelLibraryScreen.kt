package com.solarrobo.app.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solarrobo.core.ai.DeviceAiProfile
import com.solarrobo.core.ai.ModelInstallState
import com.solarrobo.core.ai.ModelStatus
import com.solarrobo.core.ai.TernaryModelCatalog
import com.solarrobo.core.ai.TernaryModelRepository
import com.solarrobo.core.ui.components.SolarCard
import com.solarrobo.core.ui.theme.SolarAmber
import com.solarrobo.core.ui.theme.SolarEmerald
import com.solarrobo.core.ui.theme.SolarRoboTheme
import com.solarrobo.core.ui.theme.SolarRose
import com.solarrobo.core.ui.theme.TextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelLibraryViewModel @Inject constructor(
    private val repository: TernaryModelRepository
) : ViewModel() {
    private val _profile = MutableStateFlow(repository.profile())
    val profile: StateFlow<DeviceAiProfile> = _profile.asStateFlow()
    val statuses: StateFlow<List<ModelStatus>> = repository.statuses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun download(id: String) {
        repositoryStatus(id)?.let { status -> viewModelScope.launch { repository.download(status.model) } }
    }

    fun activate(id: String) {
        repositoryStatus(id)?.let { repository.activate(it.model) }
    }

    private fun repositoryStatus(id: String): ModelStatus? = statuses.value.firstOrNull { it.model.id == id }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelLibraryScreen(
    viewModel: ModelLibraryViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val statuses by viewModel.statuses.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Offline model library") }) },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SolarCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = SolarAmber)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("Device fit", fontWeight = FontWeight.Bold)
                            Text("${profile.memoryClassMb} MB app memory • ${profile.abi}", color = TextSecondary)
                            Text("Recommended: ${TernaryModelCatalog.byId(profile.recommendedModelId)?.name ?: "Robo fallback"}", color = SolarEmerald)
                        }
                    }
                }
            }
            item { Text("Download one model, then make it active for Robo Talk. Models stay in app-private storage.", color = TextSecondary) }
            items(statuses, key = { it.model.id }) { status ->
                ModelCard(status, onDownload = { viewModel.download(status.model.id) }, onActivate = { viewModel.activate(status.model.id) })
            }
            item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") } }
        }
    }
}

@Composable
private fun ModelCard(status: ModelStatus, onDownload: () -> Unit, onActivate: () -> Unit) {
    SolarCard {
        Text(status.model.name, fontWeight = FontWeight.Bold)
        Text("${status.model.publisher} • ${status.model.license} • ${status.model.parameterLabel}", color = TextSecondary)
        Spacer(Modifier.padding(3.dp))
        Text(status.model.description, color = TextSecondary)
        Text("${status.model.minimumRamMb} MB minimum • ${status.model.contextLength}-token context", color = TextSecondary)
        if (status.state == ModelInstallState.DOWNLOADING) {
            LinearProgressIndicator(progress = { status.progress }, modifier = Modifier.fillMaxWidth(), color = SolarAmber)
            Text("Downloading ${(status.progress * 100).toInt()}%", color = SolarAmber)
        } else if (status.state == ModelInstallState.FAILED) {
            Text(status.error ?: "Download failed", color = SolarRose)
        }
        Spacer(Modifier.padding(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (status.state) {
                ModelInstallState.AVAILABLE, ModelInstallState.FAILED -> Button(onClick = onDownload, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                    Text("Download", modifier = Modifier.padding(start = 6.dp))
                }
                ModelInstallState.DOWNLOADING -> { CircularProgressIndicator(modifier = Modifier.padding(8.dp)) }
                ModelInstallState.INSTALLED -> Button(onClick = onActivate, modifier = Modifier.weight(1f)) { Text("Use this model") }
                ModelInstallState.ACTIVE -> Text("Active for Robo Talk", color = SolarEmerald, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ModelLibraryPreview() {
    SolarRoboTheme { ModelLibraryScreen() }
}

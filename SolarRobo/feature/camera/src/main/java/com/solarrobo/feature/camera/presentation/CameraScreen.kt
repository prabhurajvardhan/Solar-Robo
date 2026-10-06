package com.solarrobo.feature.camera.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solarrobo.feature.camera.components.CameraStatus
import com.solarrobo.feature.camera.components.CameraViewport
import com.solarrobo.feature.camera.domain.CameraConnectionState
import com.solarrobo.feature.camera.mock.FakeCameraRepository

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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CameraStatus(connectionState = state.connectionState)
            CameraViewport(
                isStreaming = state.connectionState == CameraConnectionState.STREAMING,
                modifier = Modifier.weight(1f)
            )

            if (state.errorMessage != null) {
                Text(text = state.errorMessage)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.connectionState != CameraConnectionState.STREAMING) {
                    Button(
                        onClick = { viewModel.startCamera() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start Live Feed")
                    }
                } else {
                    Button(
                        onClick = { viewModel.stopCamera() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pause Feed")
                    }
                    Button(
                        onClick = { viewModel.takeSnapshot() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Snapshot")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CameraScreenPreview() {
    val viewModel = CameraViewModel(FakeCameraRepository())
    CameraScreen(viewModel = viewModel)
}

package com.solarrobo.feature.camera.presentation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember(context) { PreviewView(context) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> viewModel.onPermissionResult(granted) }
    )

    LaunchedEffect(previewView, lifecycleOwner) {
        viewModel.bindPreview(previewView, lifecycleOwner)
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            viewModel.stopCamera()
        }
    }

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
                previewView = previewView,
                isStreaming = state.connectionState == CameraConnectionState.STREAMING,
                modifier = Modifier.weight(1f)
            )

            if (state.lastCapturedSnapshot != null) {
                val bitmap = state.lastCapturedSnapshot.jpegBytes.takeIf { it.isNotEmpty() }
                    ?.let { bytes -> android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Captured camera snapshot",
                        modifier = Modifier
                            .fillMaxWidth()
                            .size(180.dp)
                    )
                }
            }

            if (state.errorMessage != null) {
                Text(text = state.errorMessage)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!state.permissionGranted) {
                    Button(
                        onClick = { launcher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Allow Camera")
                    }
                } else if (state.connectionState != CameraConnectionState.STREAMING) {
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
    viewModel.onPermissionResult(true)
    CameraScreen(viewModel = viewModel)
}

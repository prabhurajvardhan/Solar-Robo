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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
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
import kotlin.math.roundToInt

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
        topBar = { TopAppBar(title = { Text("ASTRA Camera") }) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CameraStatus(connectionState = state.connectionState)
            CameraViewport(
                previewView = previewView,
                isStreaming = state.connectionState == CameraConnectionState.STREAMING,
                modifier = Modifier.height(240.dp)
            )
            Text("Image analysis target: 320 × 240 · measured rate: ${state.analysis.framesPerSecond} frames/s")
            Text("Mean image luminance: ${state.analysis.meanLuma?.let { "%.1f/255 · image statistic".format(it) } ?: "Not available"} · not irradiance")
            val exposureRange = state.analysis.exposureRange
            val exposureAvailable = state.analysis.exposureAdjustmentAvailable
            val sliderMin = if (exposureAvailable) exposureRange.first.toFloat() else -1f
            val sliderMax = if (exposureAvailable) exposureRange.last.toFloat() else 1f
            Text("Exposure compensation · ${if (exposureAvailable) "supported range ${exposureRange.first}..${exposureRange.last}" else "not reported by this camera"}")
            Slider(
                value = state.analysis.requestedExposureIndex.toFloat().coerceIn(sliderMin, sliderMax),
                onValueChange = { viewModel.setExposureCompensation(it.roundToInt()) },
                valueRange = sliderMin..sliderMax,
                steps = if (exposureAvailable) (exposureRange.last - exposureRange.first - 1).coerceAtLeast(0) else 0,
                enabled = exposureAvailable
            )
            Text("Requested exposure index ${state.analysis.requestedExposureIndex}; device response is best effort. Frames are processed locally.", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)

            val snapshot = state.lastCapturedSnapshot
            if (snapshot != null) {
                Text("Snapshot dimensions: ${snapshot.width} × ${snapshot.height} px (from camera frame)")
                val bitmap = snapshot.jpegBytes.takeIf { it.isNotEmpty() }
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

            val errorMessage = state.errorMessage
            if (errorMessage != null) {
                Text(text = errorMessage)
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

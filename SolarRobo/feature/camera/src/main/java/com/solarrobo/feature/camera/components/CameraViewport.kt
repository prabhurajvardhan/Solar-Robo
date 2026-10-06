package com.solarrobo.feature.camera.components

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CameraViewport(
    previewView: PreviewView,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(if (isStreaming) Color.Black else Color(0xFF1E1E1E)),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        if (!isStreaming) {
            Text("Camera Feed Inactive", color = Color.Gray)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CameraViewportPreview() {
    val context = LocalContext.current
    MaterialTheme {
        CameraViewport(
            previewView = PreviewView(context),
            isStreaming = false
        )
    }
}

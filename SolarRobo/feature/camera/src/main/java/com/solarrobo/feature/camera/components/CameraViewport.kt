package com.solarrobo.feature.camera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun CameraViewport(
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
        if (isStreaming) {
            Text("CameraX Real-Time Video Surface (1080p)", color = Color.White)
        } else {
            Text("Camera Feed Inactive", color = Color.Gray)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CameraViewportPreview() {
    MaterialTheme {
        CameraViewport(isStreaming = true)
    }
}

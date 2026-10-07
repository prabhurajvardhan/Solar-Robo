package com.solarrobo.feature.camera.domain

import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
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
    fun bindPreview(previewView: PreviewView, lifecycleOwner: LifecycleOwner): Result<Unit>
    suspend fun startStream(): Result<Unit>
    suspend fun stopStream()
    suspend fun captureSnapshot(): Result<CameraFrame>
}

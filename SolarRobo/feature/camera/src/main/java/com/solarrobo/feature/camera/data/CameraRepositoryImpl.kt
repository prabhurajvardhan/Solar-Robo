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
        val frame = CameraFrame(
            id = UUID.randomUUID().toString(),
            width = 1920,
            height = 1080,
            timestamp = System.currentTimeMillis(),
            jpegBytes = ByteArray(0)
        )
        return Result.success(frame)
    }
}

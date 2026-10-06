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

    override suspend fun stopStream() = Unit

    override suspend fun captureSnapshot(): Result<CameraFrame> = Result.success(
        CameraFrame(
            id = UUID.randomUUID().toString(),
            width = 1920,
            height = 1080,
            timestamp = System.currentTimeMillis(),
            jpegBytes = ByteArray(0)
        )
    )
}

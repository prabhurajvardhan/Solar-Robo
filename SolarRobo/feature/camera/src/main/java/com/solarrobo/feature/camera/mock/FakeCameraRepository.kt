package com.solarrobo.feature.camera.mock

import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.solarrobo.feature.camera.domain.CameraConnectionState
import com.solarrobo.feature.camera.domain.CameraAnalysisState
import com.solarrobo.feature.camera.domain.CameraFrame
import com.solarrobo.feature.camera.domain.CameraRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

class FakeCameraRepository : CameraRepository {
    override fun observeConnectionState(): Flow<CameraConnectionState> = flowOf(CameraConnectionState.STREAMING)
    override fun observeAnalysisState(): Flow<CameraAnalysisState> = flowOf(CameraAnalysisState())

    override fun bindPreview(previewView: PreviewView, lifecycleOwner: LifecycleOwner): Result<Unit> = Result.success(Unit)

    override suspend fun startStream(): Result<Unit> = Result.success(Unit)

    override suspend fun stopStream() = Unit

    override suspend fun captureSnapshot(): Result<CameraFrame> = Result.success(
        CameraFrame(
            id = UUID.randomUUID().toString(),
            width = 1920,
            height = 1080,
            timestamp = System.currentTimeMillis(),
            jpegBytes = byteArrayOf(1, 2, 3)
        )
    )

    override fun setExposureCompensation(index: Int): Result<Unit> =
        Result.failure(UnsupportedOperationException("Exposure adjustment is unavailable in the preview fake."))
}

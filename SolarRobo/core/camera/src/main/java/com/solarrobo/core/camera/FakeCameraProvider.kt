package com.solarrobo.core.camera

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeCameraProvider(
    initialStatus: CameraStatus = CameraStatus.READY
) : CameraProvider {

    private val _status = MutableStateFlow(initialStatus)

    override fun observeStatus(): Flow<CameraStatus> = _status.asStateFlow()

    override suspend fun captureSnapshot(): Result<ByteArray> {
        // Return dummy bytes representing a simulated 1x1 bitmap
        return Result.success(ByteArray(0))
    }

    fun setStatus(status: CameraStatus) {
        _status.value = status
    }
}

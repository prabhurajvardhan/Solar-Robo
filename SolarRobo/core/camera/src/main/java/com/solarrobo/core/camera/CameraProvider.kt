package com.solarrobo.core.camera

import kotlinx.coroutines.flow.Flow

/**
 * Camera hardware abstraction contract for optical inspection and panel alignment.
 */
interface CameraProvider {
    fun observeStatus(): Flow<CameraStatus>
    suspend fun captureSnapshot(): Result<ByteArray>
}

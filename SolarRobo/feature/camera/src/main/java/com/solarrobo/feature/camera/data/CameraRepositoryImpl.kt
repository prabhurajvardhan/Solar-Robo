package com.solarrobo.feature.camera.data

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.solarrobo.feature.camera.domain.CameraConnectionState
import com.solarrobo.feature.camera.domain.CameraFrame
import com.solarrobo.feature.camera.domain.CameraRepository
import com.solarrobo.feature.camera.platform.CameraXController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CameraRepository {
    private val state = MutableStateFlow(CameraConnectionState.DISCONNECTED)
    private val controller = CameraXController(context)

    override fun observeConnectionState(): Flow<CameraConnectionState> = state.asStateFlow()

    override fun bindPreview(previewView: PreviewView, lifecycleOwner: LifecycleOwner): Result<Unit> {
        val bindResult = controller.bindPreview(previewView, lifecycleOwner)
        state.value = controller.currentState()
        return bindResult
    }

    override suspend fun startStream(): Result<Unit> {
        return controller.start().onSuccess {
            state.value = CameraConnectionState.STREAMING
        }.onFailure {
            state.value = CameraConnectionState.ERROR
        }
    }

    override suspend fun stopStream() {
        controller.stop()
        state.value = CameraConnectionState.DISCONNECTED
    }

    override suspend fun captureSnapshot(): Result<CameraFrame> {
        val result = controller.captureSnapshot()
        result.onFailure {
            state.value = CameraConnectionState.ERROR
        }
        return result
    }
}

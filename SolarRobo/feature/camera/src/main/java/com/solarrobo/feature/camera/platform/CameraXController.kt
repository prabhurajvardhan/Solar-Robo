package com.solarrobo.feature.camera.platform

import android.content.Context
import android.graphics.ImageFormat
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.solarrobo.feature.camera.domain.CameraConnectionState
import com.solarrobo.feature.camera.domain.CameraFrame
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume

class CameraXController(
    private val context: Context
) {
    private var previewView: PreviewView? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var connectedState: CameraConnectionState = CameraConnectionState.DISCONNECTED

    fun bindPreview(previewView: PreviewView, lifecycleOwner: LifecycleOwner): Result<Unit> {
        this.previewView = previewView
        this.lifecycleOwner = lifecycleOwner
        return Result.success(Unit)
    }

    fun currentState(): CameraConnectionState = connectedState

    fun start(): Result<Unit> {
        val owner = lifecycleOwner ?: return Result.failure(IllegalStateException("Camera lifecycle is not attached."))
        val view = previewView ?: return Result.failure(IllegalStateException("Camera preview is not ready."))

        return try {
            val provider = ProcessCameraProvider.getInstance(context)
            cameraProvider = provider.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(view.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setTargetRotation(view.display?.rotation ?: 0)
                .setTargetFormat(ImageFormat.JPEG)
                .build()

            cameraProvider?.unbindAll()
            camera = cameraProvider?.bindToLifecycle(
                owner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
            connectedState = CameraConnectionState.STREAMING
            Result.success(Unit)
        } catch (t: Throwable) {
            connectedState = CameraConnectionState.ERROR
            Result.failure(t)
        }
    }

    fun stop() {
        cameraProvider?.unbindAll()
        camera = null
        imageCapture = null
        connectedState = CameraConnectionState.DISCONNECTED
    }

    suspend fun captureSnapshot(): Result<CameraFrame> = suspendCancellableCoroutine { continuation ->
        val capture = imageCapture ?: run {
            continuation.resume(Result.failure(IllegalStateException("Camera is not active.")))
            return@suspendCancellableCoroutine
        }

        val outputFile = File(context.cacheDir, "capture_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val bytes = outputFile.readBytes()
                    val frame = CameraFrame(
                        id = UUID.randomUUID().toString(),
                        width = 1920,
                        height = 1080,
                        timestamp = System.currentTimeMillis(),
                        jpegBytes = bytes
                    )
                    continuation.resume(Result.success(frame))
                }

                override fun onError(exception: ImageCaptureException) {
                    continuation.resume(Result.failure(exception))
                }
            }
        )
    }
}

package com.solarrobo.feature.camera.platform

import android.content.Context
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.solarrobo.feature.camera.domain.CameraAnalysisState
import com.solarrobo.feature.camera.domain.CameraConnectionState
import com.solarrobo.feature.camera.domain.CameraFrame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume

class CameraXController(private val context: Context) {
    private var previewView: PreviewView? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var connectedState: CameraConnectionState = CameraConnectionState.DISCONNECTED
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor { r -> Thread(r, "astra-camera-analysis") }
    private val _analysisState = MutableStateFlow(CameraAnalysisState())
    val analysisState = _analysisState.asStateFlow()
    private var fpsWindowStartMs = System.currentTimeMillis()
    private var framesInWindow = 0

    fun bindPreview(previewView: PreviewView, lifecycleOwner: LifecycleOwner): Result<Unit> {
        this.previewView = previewView
        this.lifecycleOwner = lifecycleOwner
        return Result.success(Unit)
    }

    fun currentState(): CameraConnectionState = connectedState

    suspend fun start(): Result<Unit> {
        val owner = lifecycleOwner ?: return Result.failure(IllegalStateException("Camera lifecycle is not attached."))
        val view = previewView ?: return Result.failure(IllegalStateException("Camera preview is not ready."))
        return try {
            val future = ProcessCameraProvider.getInstance(context)
            val provider = suspendCancellableCoroutine { continuation ->
                future.addListener({
                    if (continuation.isActive) {
                        runCatching { future.get() }
                            .onSuccess { continuation.resume(it) }
                            .onFailure { continuation.resumeWith(Result.failure(it)) }
                    }
                }, ContextCompat.getMainExecutor(context))
            }
            unbindOwnedUseCases()
            val previewCase = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
            val captureCase = ImageCapture.Builder()
                .setTargetRotation(view.display?.rotation ?: 0)
                .build()
            @Suppress("DEPRECATION")
            val analysisCase = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(Size(320, 240))
                .build()
            val boundCamera = provider.bindToLifecycle(
                owner, CameraSelector.DEFAULT_BACK_CAMERA, previewCase, captureCase, analysisCase
            )
            this.cameraProvider = provider
            this.preview = previewCase
            this.imageCapture = captureCase
            this.imageAnalysis = analysisCase
            this.camera = boundCamera
            connectedState = CameraConnectionState.STREAMING
            val exposure = boundCamera.cameraInfo.exposureState
            val range = exposure.exposureCompensationRange
            _analysisState.value = CameraAnalysisState(
                exposureRange = range.lower..range.upper,
                requestedExposureIndex = exposure.exposureCompensationIndex
            )
            fpsWindowStartMs = System.currentTimeMillis()
            framesInWindow = 0
            analysisCase.setAnalyzer(analysisExecutor) { image -> analyzeAndClose(image) }
            Result.success(Unit)
        } catch (error: Throwable) {
            stop()
            connectedState = CameraConnectionState.ERROR
            Result.failure(error)
        }
    }

    fun stop() {
        imageAnalysis?.clearAnalyzer()
        unbindOwnedUseCases()
        connectedState = CameraConnectionState.DISCONNECTED
        _analysisState.value = CameraAnalysisState()
    }

    private fun unbindOwnedUseCases() {
        val uses = listOfNotNull(preview, imageCapture, imageAnalysis)
        if (uses.isNotEmpty()) cameraProvider?.unbind(*uses.toTypedArray())
        preview = null
        imageCapture = null
        imageAnalysis = null
        camera = null
    }

    fun setExposureCompensation(index: Int): Result<Unit> {
        val boundCamera = camera ?: return Result.failure(IllegalStateException("Camera is not active."))
        val range = boundCamera.cameraInfo.exposureState.exposureCompensationRange
        if (index !in range.lower..range.upper) {
            return Result.failure(IllegalArgumentException("Exposure compensation must be in ${range.lower}..${range.upper}."))
        }
        return runCatching {
            boundCamera.cameraControl.setExposureCompensationIndex(index)
            _analysisState.value = _analysisState.value.copy(requestedExposureIndex = index)
        }
    }

    suspend fun captureSnapshot(): Result<CameraFrame> = suspendCancellableCoroutine { continuation ->
        val capture = imageCapture
        if (capture == null) {
            continuation.resume(Result.failure(IllegalStateException("Camera is not active.")))
            return@suspendCancellableCoroutine
        }
        capture.takePicture(analysisExecutor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val bytes = image.planes.firstOrNull()?.buffer?.let(::copyBytes)
                        ?: throw IllegalStateException("Camera did not return image bytes.")
                    val frame = CameraFrame(
                        id = UUID.randomUUID().toString(), width = image.width, height = image.height,
                        timestamp = System.currentTimeMillis(), jpegBytes = bytes
                    )
                    if (continuation.isActive) continuation.resume(Result.success(frame))
                } catch (error: Throwable) {
                    if (continuation.isActive) continuation.resume(Result.failure(error))
                } finally {
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                if (continuation.isActive) continuation.resume(Result.failure(exception))
            }
        })
    }

    private fun analyzeAndClose(image: ImageProxy) {
        try {
            val luma = meanLuma(image)
            _analysisState.value = _analysisState.value.copy(meanLuma = luma)
            framesInWindow++
            val now = System.currentTimeMillis()
            val elapsed = now - fpsWindowStartMs
            if (elapsed >= 1_000L) {
                _analysisState.value = _analysisState.value.copy(
                    framesPerSecond = (framesInWindow * 1_000.0 / elapsed).toInt()
                )
                fpsWindowStartMs = now
                framesInWindow = 0
            }
        } catch (_: Throwable) {
            // A bad frame is discarded; CameraX continues with the next latest frame.
        } finally {
            image.close()
        }
    }

    /** Fast sparse luma sample; this is an image statistic, not an irradiance measurement. */
    private fun meanLuma(image: ImageProxy): Float? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer.duplicate()
        val xStep = (image.width / 40).coerceAtLeast(1)
        val yStep = (image.height / 30).coerceAtLeast(1)
        var sum = 0L
        var count = 0
        for (y in 0 until image.height step yStep) {
            for (x in 0 until image.width step xStep) {
                val offset = y * plane.rowStride + x * plane.pixelStride
                if (offset < buffer.limit()) {
                    sum += buffer.get(offset).toInt() and 0xff
                    count++
                }
            }
        }
        return if (count == 0) null else sum.toFloat() / count
    }

    private fun copyBytes(source: ByteBuffer): ByteArray {
        val buffer = source.duplicate()
        return ByteArray(buffer.remaining()).also(buffer::get)
    }
}

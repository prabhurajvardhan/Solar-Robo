package com.solarrobo.core.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.solarrobo.core.tracker.FrameSource
import com.solarrobo.core.tracker.RgbFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Lifecycle-bound CameraX analyzer. Frames are downscaled and only the newest pending frame is retained. */
class CameraXFrameSource(
    private val context: android.content.Context,
    private val targetWidth: Int = 320,
    private val targetHeight: Int = 240,
) : FrameSource, AutoCloseable {
    private val frameMutex = Mutex()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r -> Thread(r, "camera-analysis") }
    private val _status = MutableStateFlow(CameraStatus.IDLE)
    val status: Flow<CameraStatus> = _status.asStateFlow()
    private val _exposureControlReport = MutableStateFlow("Camera exposure controls not checked")
    val exposureControlReport: Flow<String> = _exposureControlReport.asStateFlow()
    /** Optional local-only observer; exceptions are isolated from the tracker frame path. */
    @Volatile var onFrameObserved: ((RgbFrame) -> Unit)? = null

    @Volatile private var provider: ProcessCameraProvider? = null
    @Volatile private var analysis: ImageAnalysis? = null
    @Volatile private var owner: LifecycleOwner? = null
    @Volatile private var stateObserver: Observer<androidx.camera.core.CameraState>? = null
    @Volatile private var cameraState: androidx.lifecycle.LiveData<androidx.camera.core.CameraState>? = null
    private val closed = AtomicBoolean(false)

    /** Bind the back camera to [owner]. Calling start again first unbinds this source's use case. */
    @Synchronized
    fun start(owner: LifecycleOwner) {
        check(!closed.get()) { "CameraXFrameSource is closed" }
        stopBinding()
        this.owner = owner
        _status.value = CameraStatus.IDLE
        try {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                try {
                    val cameraProvider = future.get()
                    if (this.owner !== owner || closed.get()) return@addListener
                    val builder = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    try {
                        val backInfo = cameraProvider.availableCameraInfos.firstOrNull { info ->
                            Camera2CameraInfo.from(info).getCameraCharacteristic(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
                        }
                        if (backInfo == null) {
                            _exposureControlReport.value = "Back camera exposure controls unavailable"
                        } else {
                            val info = Camera2CameraInfo.from(backInfo)
                            val aeLock = info.getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE) == true
                            val compensation = info.getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
                            val capabilities = info.getCameraCharacteristic(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                            val exposureRange = info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
                            val sensitivityRange = info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
                            val manualSensor = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) == true &&
                                exposureRange != null && sensitivityRange != null
                            val extender = Camera2Interop.Extender(builder)
                            if (manualSensor) {
                                extender.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                                extender.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureRange!!.lower)
                                extender.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, sensitivityRange!!.lower)
                            } else {
                                if (aeLock) extender.setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, true)
                                if (compensation != null && compensation.lower < 0) {
                                    extender.setCaptureRequestOption(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, compensation.lower)
                                }
                            }
                            _exposureControlReport.value = when {
                                manualSensor -> "Requested shortest supported exposure (${exposureRange!!.lower} ns) and lowest sensitivity (${sensitivityRange!!.lower}); device support is best effort"
                                aeLock && compensation != null && compensation.lower < 0 -> "Requested AE lock and minimum compensation (${compensation.lower}); device support is best effort"
                                aeLock -> "Requested AE lock; exposure compensation not supported"
                                compensation != null && compensation.lower < 0 -> "Requested minimum exposure compensation (${compensation.lower}); AE lock not supported"
                                else -> "Back camera does not report AE lock or negative exposure compensation support"
                            }
                        }
                    } catch (_: Throwable) {
                        _exposureControlReport.value = "Camera2 exposure controls unavailable; using device defaults"
                    }
                    val useCase = builder.build()
                    val camera = cameraProvider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, useCase)
                    provider = cameraProvider
                    analysis = useCase
                    val observer = Observer<androidx.camera.core.CameraState> { state ->
                        _status.value = when {
                            state == null -> CameraStatus.ERROR
                            state.error != null -> CameraStatus.ERROR
                            state.type == androidx.camera.core.CameraState.Type.OPEN -> CameraStatus.STREAMING
                            else -> CameraStatus.READY
                        }
                    }
                    stateObserver = observer
                    cameraState = camera.cameraInfo.cameraState
                    cameraState?.observe(owner, observer)
                    _status.value = CameraStatus.READY
                } catch (_: Throwable) {
                    _status.value = CameraStatus.ERROR
                }
            }, androidx.core.content.ContextCompat.getMainExecutor(context))
        } catch (_: Throwable) {
            _status.value = CameraStatus.UNAVAILABLE
        }
    }

    /** Stop capture and release the lifecycle-bound use case; queued frames are discarded. */
    @Synchronized
    fun stop() {
        stopBinding()
        _status.value = CameraStatus.IDLE
    }

    @Synchronized
    private fun stopBinding() {
        val cameraProvider = provider
        val useCase = analysis
        if (useCase != null) useCase.clearAnalyzer()
        if (cameraProvider != null && useCase != null) cameraProvider.unbind(useCase)
        val observer = stateObserver
        val boundOwner = owner
        if (observer != null && boundOwner != null) cameraState?.removeObserver(observer)
        analysis = null
        provider = null
        stateObserver = null
        cameraState = null
        owner = null
    }

    /** Install the analyzer only while a consumer requests a frame, then immediately clear it. */
    override suspend fun nextFrame(): RgbFrame? = frameMutex.withLock {
        val useCase = analysis ?: return@withLock null
        val result = CompletableDeferred<RgbFrame?>()
        _status.value = CameraStatus.STREAMING
        useCase.setAnalyzer(executor) { image ->
            try {
                if (result.isActive) {
                    val frame = Yuv420888Converter.convert(image, targetWidth, targetHeight)
                    result.complete(frame)
                    runCatching { onFrameObserved?.invoke(frame) }
                }
            } catch (error: Throwable) {
                result.completeExceptionally(error)
                _status.value = CameraStatus.ERROR
            } finally {
                image.close()
            }
        }
        try {
            result.await()
        } finally {
            useCase.clearAnalyzer()
            if (_status.value != CameraStatus.ERROR) _status.value = CameraStatus.READY
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            stop()
            executor.shutdown()
        }
    }
}

/** Converts planar/semiplanar YUV_420_888 planes to row-major interleaved RGB, nearest-neighbor downscaled. */
internal object Yuv420888Converter {
    fun convert(image: ImageProxy, targetWidth: Int, targetHeight: Int): RgbFrame {
        require(image.format == android.graphics.ImageFormat.YUV_420_888)
        val scale = minOf(1f, targetWidth.toFloat() / image.width, targetHeight.toFloat() / image.height)
        val outWidth = (image.width * scale).toInt().coerceAtLeast(1)
        val outHeight = (image.height * scale).toInt().coerceAtLeast(1)
        val y = image.planes[0]
        val u = image.planes[1]
        val v = image.planes[2]
        val yBuf = y.buffer.duplicate()
        val uBuf = u.buffer.duplicate()
        val vBuf = v.buffer.duplicate()
        val output = ByteArray(outWidth * outHeight * 3)
        var out = 0
        for (dy in 0 until outHeight) {
            val sy = dy * image.height / outHeight
            for (dx in 0 until outWidth) {
                val sx = dx * image.width / outWidth
                val yp = sy * y.rowStride + sx * y.pixelStride
                val uvX = sx / 2
                val uvY = sy / 2
                val up = uvY * u.rowStride + uvX * u.pixelStride
                val vp = uvY * v.rowStride + uvX * v.pixelStride
                val yy = (yBuf.get(yp).toInt() and 255) - 16
                val uu = (uBuf.get(up).toInt() and 255) - 128
                val vv = (vBuf.get(vp).toInt() and 255) - 128
                val c = (yy.coerceAtLeast(0) * 298)
                output[out++] = ((c + 409 * vv + 128 shr 8).coerceIn(0, 255)).toByte()
                output[out++] = ((c - 100 * uu - 208 * vv + 128 shr 8).coerceIn(0, 255)).toByte()
                output[out++] = ((c + 516 * uu + 128 shr 8).coerceIn(0, 255)).toByte()
            }
        }
        return RgbFrame(outWidth, outHeight, output)
    }
}

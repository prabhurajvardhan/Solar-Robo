package com.solarrobo.core.weather

import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

/** Canonical output labels, in the exact order used by the training export. */
enum class SkyClass(val label: String) {
    CLEAR("clear"),
    FAIR_CUMULUS("fair_weather_cumulus"),
    STRATUS("overcast_or_stratus"),
    TOWERING_CUMULUS("towering_cumulus"),
    CUMULONIMBUS("cumulonimbus_or_storm_like"),
    FOG("fog_or_obscured_sky"),
    PRECIPITATION("rain_or_precipitation");

    companion object {
        val labels: List<String> = entries.map { it.label }
    }
}

data class SkyPrediction(
    val label: String,
    val confidence: Float,
    val accepted: Boolean,
    val observedAtEpochMillis: Long
)

/**
 * CPU LiteRT inference. Input pixels are interleaved RGB bytes; aspect ratio is preserved with black
 * padding. Input preprocessing (0..255 floats to MobileNetV3's [-1,1]) is embedded in the model.
 * A score below [minimumConfidence] remains visible as a low-confidence prediction, never a fact.
 */
class LiteSkyClassifier(
    model: ByteBuffer,
    private val inputSize: Int = 160,
    private val minimumConfidence: Float = 0.55f,
    threads: Int = 2
) : AutoCloseable {
    private val interpreter = Interpreter(model.order(ByteOrder.nativeOrder()), Interpreter.Options().setNumThreads(threads))

    init {
        require(inputSize > 0 && minimumConfidence in 0f..1f)
        val shape = interpreter.getInputTensor(0).shape()
        require(shape.contentEquals(intArrayOf(1, inputSize, inputSize, 3))) {
            "Expected [1,$inputSize,$inputSize,3] float input; got ${shape.contentToString()}"
        }
        require(interpreter.getInputTensor(0).dataType() == org.tensorflow.lite.DataType.FLOAT32)
        require(interpreter.getOutputTensor(0).shape().lastOrNull() == SkyClass.entries.size) {
            "Model output count does not match the seven sky labels"
        }
    }

    @Synchronized
    fun classify(rgb: ByteArray, width: Int, height: Int, nowMillis: Long = System.currentTimeMillis()): SkyPrediction {
        require(width > 0 && height > 0 && rgb.size == width * height * 3)
        val input = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3).order(ByteOrder.nativeOrder())
        val scale = min(inputSize.toFloat() / width, inputSize.toFloat() / height)
        val resizedWidth = (width * scale).toInt().coerceIn(1, inputSize)
        val resizedHeight = (height * scale).toInt().coerceIn(1, inputSize)
        val left = (inputSize - resizedWidth) / 2
        val top = (inputSize - resizedHeight) / 2
        for (y in 0 until inputSize) for (x in 0 until inputSize) {
            val inside = x in left until left + resizedWidth && y in top until top + resizedHeight
            if (inside) {
                val sx = ((x - left) * width / resizedWidth).coerceIn(0, width - 1)
                val sy = ((y - top) * height / resizedHeight).coerceIn(0, height - 1)
                val i = (sy * width + sx) * 3
                input.putFloat((rgb[i].toInt() and 0xff).toFloat())
                input.putFloat((rgb[i + 1].toInt() and 0xff).toFloat())
                input.putFloat((rgb[i + 2].toInt() and 0xff).toFloat())
            } else {
                input.putFloat(0f); input.putFloat(0f); input.putFloat(0f)
            }
        }
        input.rewind()
        val output = Array(1) { FloatArray(SkyClass.entries.size) }
        interpreter.run(input, output)
        val scores = output[0]
        val best = scores.indices.maxBy { scores[it] }
        val confidence = scores[best].coerceIn(0f, 1f)
        return SkyPrediction(SkyClass.entries[best].label, confidence, confidence >= minimumConfidence, nowMillis)
    }

    override fun close() = interpreter.close()
}

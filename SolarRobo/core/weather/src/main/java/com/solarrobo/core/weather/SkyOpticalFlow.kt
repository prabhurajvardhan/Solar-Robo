package com.solarrobo.core.weather

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Apparent 2-D image displacement only; this is not a measurement of ground-level wind. */
data class OpticalFlowEstimate(
    val dxPixelsPerSecond: Double?,
    val dyPixelsPerSecond: Double?,
    val matchedPatches: Int,
    val confidence: Double?,
    val intervalSeconds: Double
)

/**
 * Small block-matching flow approximation for a fixed sky camera. It downsamples to at most 80x60,
 * matches textured 6x6 patches in a bounded search window, and reports the median displacement.
 * Uniform patches are ignored; fewer than three matches yields unavailable flow.
 */
object SkyOpticalFlow {
    fun estimate(
        previousRgb: ByteArray, currentRgb: ByteArray, width: Int, height: Int,
        intervalSeconds: Double, maximumDimension: Int = 80, searchRadius: Int = 4
    ): OpticalFlowEstimate {
        require(width > 0 && height > 0 && previousRgb.size == width * height * 3 && currentRgb.size == width * height * 3)
        require(intervalSeconds > 0.0 && maximumDimension >= 24 && searchRadius in 1..12)
        val scale = max(1, max(width / maximumDimension, height / (maximumDimension * 3 / 4)))
        val smallW = max(1, width / scale)
        val smallH = max(1, height / scale)
        val prev = luminance(previousRgb, width, height, smallW, smallH)
        val curr = luminance(currentRgb, width, height, smallW, smallH)
        val patch = 6
        val step = 8
        val vectors = mutableListOf<Pair<Int, Int>>()
        var totalCost = 0.0
        var costCount = 0
        for (y in (searchRadius + 2) until (smallH - searchRadius - patch) step step) {
            for (x in (searchRadius + 2) until (smallW - searchRadius - patch) step step) {
                val variance = patchVariance(prev, smallW, x, y, patch)
                if (variance < 90.0) continue
                var bestCost = Double.POSITIVE_INFINITY
                var bestDx = 0; var bestDy = 0
                for (dy in -searchRadius..searchRadius) for (dx in -searchRadius..searchRadius) {
                    val cost = patchSad(prev, curr, smallW, x, y, x + dx, y + dy, patch)
                    if (cost < bestCost) { bestCost = cost; bestDx = dx; bestDy = dy }
                }
                if (bestCost <= 55.0) {
                    vectors += bestDx to bestDy
                    totalCost += bestCost
                    costCount++
                }
            }
        }
        if (vectors.size < 3) return OpticalFlowEstimate(null, null, vectors.size, null, intervalSeconds)
        val medianX = vectors.map { it.first }.sorted()[vectors.size / 2]
        val medianY = vectors.map { it.second }.sorted()[vectors.size / 2]
        val confidence = (1.0 - (totalCost / max(1, costCount)) / 255.0).coerceIn(0.0, 1.0)
        return OpticalFlowEstimate(medianX * scale / intervalSeconds, medianY * scale / intervalSeconds, vectors.size, confidence, intervalSeconds)
    }

    private fun luminance(rgb: ByteArray, width: Int, height: Int, outW: Int, outH: Int): ByteArray {
        val out = ByteArray(outW * outH)
        for (y in 0 until outH) for (x in 0 until outW) {
            val sx = min(width - 1, x * width / outW); val sy = min(height - 1, y * height / outH)
            val i = (sy * width + sx) * 3
            val r = rgb[i].toInt() and 255; val g = rgb[i + 1].toInt() and 255; val b = rgb[i + 2].toInt() and 255
            out[y * outW + x] = ((77 * r + 150 * g + 29 * b) shr 8).toByte()
        }
        return out
    }

    private fun patchVariance(a: ByteArray, stride: Int, x: Int, y: Int, size: Int): Double {
        var sum = 0.0; var sumSq = 0.0; val n = size * size
        for (py in 0 until size) for (px in 0 until size) {
            val value = (a[(y + py) * stride + x + px].toInt() and 255).toDouble()
            sum += value; sumSq += value * value
        }
        val mean = sum / n
        return sumSq / n - mean * mean
    }

    private fun patchSad(a: ByteArray, b: ByteArray, stride: Int, ax: Int, ay: Int, bx: Int, by: Int, size: Int): Double {
        var sum = 0.0
        for (py in 0 until size) for (px in 0 until size) {
            sum += abs((a[(ay + py) * stride + ax + px].toInt() and 255) - (b[(by + py) * stride + bx + px].toInt() and 255))
        }
        return sum / (size * size)
    }
}

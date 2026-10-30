package com.solarrobo.core.tracker

/** Downscaled row-major RGB frame. [pixels] is interleaved unsigned RGB bytes. */
data class RgbFrame(val width: Int, val height: Int, val pixels: ByteArray) {
    init { require(width > 0 && height > 0 && pixels.size == width * height * 3) }
    private fun channel(index: Int): Int = pixels[index].toInt() and 0xff
    fun red(pixel: Int) = channel(pixel * 3)
    fun green(pixel: Int) = channel(pixel * 3 + 1)
    fun blue(pixel: Int) = channel(pixel * 3 + 2)
}

data class SunBlob(val centerX: Float, val centerY: Float, val areaPixels: Int)
data class SkyStats(val brightness: Float, val blueFraction: Float, val whiteFraction: Float)

object SunDetector {
    fun findSun(frame: RgbFrame, minimumPixels: Int = 4, maximumAreaFraction: Float = 0.15f): SunBlob? {
        require(minimumPixels > 0 && maximumAreaFraction in 0f..1f)
        var area = 0
        var sumX = 0L
        var sumY = 0L
        for (y in 0 until frame.height) for (x in 0 until frame.width) {
            val i = y * frame.width + x
            if (minOf(frame.red(i), frame.green(i), frame.blue(i)) >= 235) {
                area++
                sumX += x
                sumY += y
            }
        }
        if (area < minimumPixels || area > maximumAreaFraction * frame.width * frame.height) return null
        return SunBlob(sumX.toFloat() / area, sumY.toFloat() / area, area)
    }

    /** Descriptors only; these are image heuristics, not calibrated weather measurements. */
    fun skyStats(frame: RgbFrame): SkyStats {
        var lum = 0L
        var blue = 0
        var white = 0
        val count = frame.width * frame.height
        for (i in 0 until count) {
            val r = frame.red(i)
            val g = frame.green(i)
            val b = frame.blue(i)
            lum += (r + g + b) / 3
            if (b > r + 25 && b >= g) blue++
            val min = minOf(r, g, b)
            val max = maxOf(r, g, b)
            if (min > 150 && max - min < 25 && min < 235) white++
        }
        return SkyStats(lum.toFloat() / count, blue.toFloat() / count, white.toFloat() / count)
    }
}

package com.imagingutils.data.processing

import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Midtones Transfer Function (MTF) auto-stretch, matching Siril / PixInsight STF.
 * These functions are display-only: callers pass decoded pixels and receive a new
 * stretched image. The source pixels are never modified. See Autostretch.md.
 */

private const val TARGET_BACKGROUND = 0.25
private const val SHADOWS_CLIP = -2.8
private const val MAD_NORM = 1.4826
private const val MAX_SAMPLES = 500_000

/** Midtones transfer function with midtones balance [m], for x in [0, 1]. */
fun mtf(x: Double, m: Double): Double = when {
    x <= 0.0 -> 0.0
    x >= 1.0 -> 1.0
    x == m -> 0.5
    else -> ((m - 1.0) * x) / ((2.0 * m - 1.0) * x - m)
}

private class Stretch(private val shadows: Double, private val midtones: Double, private val inverted: Boolean) {
    private val denom = (1.0 - shadows).coerceAtLeast(1e-9)

    /** Maps a normalized input value in [0, 1] to a stretched value in [0, 1]. */
    fun apply(x: Double): Double = if (!inverted) {
        mtf(((x - shadows) / denom).coerceIn(0.0, 1.0), midtones)
    } else {
        1.0 - mtf(((1.0 - x - shadows) / denom).coerceIn(0.0, 1.0), midtones)
    }
}

/** Derives stretch parameters from the [median] and normalized MAD [madn] (both in [0, 1]). */
private fun paramsFrom(median: Double, madn: Double): Stretch {
    val inverted = median > 0.5
    val ref = if (inverted) 1.0 - median else median
    val shadows = (ref + SHADOWS_CLIP * madn).coerceIn(0.0, 1.0)
    val midtones = mtf(ref - shadows, TARGET_BACKGROUND)
    return Stretch(shadows, midtones, inverted)
}

private fun median(sorted: DoubleArray): Double {
    if (sorted.isEmpty()) return 0.0
    val n = sorted.size
    return if (n % 2 == 1) sorted[n / 2] else (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
}

/**
 * Auto-stretches raw single-channel [data] (row-major, [nx] x [ny]) treated as linear,
 * returning a new grayscale image. Robust stats are computed on a bounded sample.
 */
fun autostretch(data: DoubleArray, nx: Int, ny: Int): BufferedImage {
    val count = minOf(data.size, nx * ny)
    var mn = Double.MAX_VALUE
    var mx = -Double.MAX_VALUE
    for (i in 0 until count) {
        val v = data[i]
        if (v.isNaN()) continue
        if (v < mn) mn = v
        if (v > mx) mx = v
    }
    val range = if (mx > mn) mx - mn else 1.0
    val step = (count / MAX_SAMPLES).coerceAtLeast(1)
    val samples = ArrayList<Double>(count / step + 1)
    var i = 0
    while (i < count) {
        val v = data[i]
        if (!v.isNaN()) samples.add((v - mn) / range)
        i += step
    }
    val sorted = samples.toDoubleArray().also { it.sort() }
    val med = median(sorted)
    val dev = DoubleArray(sorted.size) { abs(sorted[it] - med) }.also { it.sort() }
    val madn = MAD_NORM * median(dev)
    val stretch = paramsFrom(med, madn)

    val img = BufferedImage(nx, ny, BufferedImage.TYPE_INT_RGB)
    var idx = 0
    for (y in 0 until ny) {
        for (x in 0 until nx) {
            if (idx >= count) break
            val v = data[idx++]
            val xn = if (v.isNaN()) 0.0 else (v - mn) / range
            val g = (stretch.apply(xn) * 255.0).roundToInt().coerceIn(0, 255)
            img.setRGB(x, y, (g shl 16) or (g shl 8) or g)
        }
    }
    return img
}

/** Auto-stretches an already-decoded 8-bit image (linked channels), returning a new copy. */
fun autostretch(src: BufferedImage): BufferedImage {
    val w = src.width
    val h = src.height
    val hist = IntArray(256)
    val lum = IntArray(w * h)
    var p = 0
    for (y in 0 until h) {
        for (x in 0 until w) {
            val rgb = src.getRGB(x, y)
            val r = (rgb ushr 16) and 0xFF
            val g = (rgb ushr 8) and 0xFF
            val b = rgb and 0xFF
            val l = (0.2126 * r + 0.7152 * g + 0.0722 * b).roundToInt().coerceIn(0, 255)
            lum[p++] = l
            hist[l]++
        }
    }
    val total = w * h
    val medL = percentile(hist, total)
    val madHist = IntArray(256)
    for (l in lum) madHist[abs(l - medL)]++
    val madL = percentile(madHist, total)
    val stretch = paramsFrom(medL / 255.0, MAD_NORM * (madL / 255.0))

    val lut = IntArray(256) { (stretch.apply(it / 255.0) * 255.0).roundToInt().coerceIn(0, 255) }
    val dst = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    for (y in 0 until h) {
        for (x in 0 until w) {
            val rgb = src.getRGB(x, y)
            val r = lut[(rgb ushr 16) and 0xFF]
            val g = lut[(rgb ushr 8) and 0xFF]
            val b = lut[rgb and 0xFF]
            dst.setRGB(x, y, (r shl 16) or (g shl 8) or b)
        }
    }
    return dst
}

/** Returns the bin index at the 50th percentile of a histogram. */
private fun percentile(hist: IntArray, total: Int): Int {
    val half = total / 2
    var cum = 0
    for (i in hist.indices) {
        cum += hist[i]
        if (cum >= half) return i
    }
    return hist.size - 1
}

package com.imagingutils.data

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.imagingutils.data.processing.autostretch
import nom.tam.fits.Fits
import nom.tam.util.ArrayFuncs
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min

/**
 * Produces a downscaled thumbnail for [entry], or null when the format cannot be
 * decoded yet (RAW/XISF are handled with a placeholder in the UI for now).
 */
fun loadThumbnail(entry: ImageEntry, maxSize: Int = 256): ImageBitmap? {
    val image: BufferedImage = when (entry.kind) {
        ImageKind.PHOTO -> runCatching { ImageIO.read(entry.file) }.getOrNull()
        ImageKind.FITS -> runCatching { renderFits(entry.file) }.getOrNull()
        else -> null
    } ?: return null
    return scale(image, maxSize).toComposeImageBitmap()
}

/**
 * Produces an auto-stretched (MTF/STF) preview for [entry], scaled to [maxSize].
 * The stretch is applied to an in-memory copy only; the source file is untouched.
 * Returns null for formats that cannot be decoded (RAW/XISF).
 */
fun loadAutostretchPreview(entry: ImageEntry, maxSize: Int = 1600): ImageBitmap? {
    val stretched: BufferedImage = when (entry.kind) {
        ImageKind.PHOTO -> runCatching {
            ImageIO.read(entry.file)?.let { autostretch(scale(it, maxSize)) }
        }.getOrNull()
        ImageKind.FITS -> runCatching {
            readFitsData(entry.file)?.let { autostretch(it.data, it.nx, it.ny) }
        }.getOrNull()
        else -> null
    } ?: return null
    return scale(stretched, maxSize).toComposeImageBitmap()
}

private fun scale(src: BufferedImage, maxSize: Int): BufferedImage {
    val w = src.width
    val h = src.height
    if (w <= maxSize && h <= maxSize) return toRgb(src)
    val ratio = min(maxSize.toDouble() / w, maxSize.toDouble() / h)
    val nw = max(1, (w * ratio).toInt())
    val nh = max(1, (h * ratio).toInt())
    val dst = BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB)
    val g = dst.createGraphics()
    g.drawImage(src.getScaledInstance(nw, nh, Image.SCALE_SMOOTH), 0, 0, null)
    g.dispose()
    return dst
}

private fun toRgb(src: BufferedImage): BufferedImage {
    if (src.type == BufferedImage.TYPE_INT_RGB) return src
    val dst = BufferedImage(src.width, src.height, BufferedImage.TYPE_INT_RGB)
    val g = dst.createGraphics()
    g.drawImage(src, 0, 0, null)
    g.dispose()
    return dst
}

/** Raw (linear) pixel data for the first 2D image plane of a FITS file. */
internal class FitsData(val data: DoubleArray, val nx: Int, val ny: Int)

/** Reads the first 2D image plane of a FITS file as raw (linear) pixel data. */
internal fun readFitsData(file: File): FitsData? {
    Fits(file).use { fits ->
        val hdu = fits.read().firstOrNull { it.axes != null && it.axes.size >= 2 } ?: return null
        val axes = hdu.axes
        val ny = axes[axes.size - 2]
        val nx = axes[axes.size - 1]
        val data = toDoubleArray(ArrayFuncs.flatten(hdu.kernel)) ?: return null
        return FitsData(data, nx, ny)
    }
}

/** Renders the first 2D image plane of a FITS file to a normalized grayscale image. */
private fun renderFits(file: File): BufferedImage? {
    val fd = readFitsData(file) ?: return null
    val data = fd.data
    val nx = fd.nx
    val ny = fd.ny
    val count = min(data.size, nx * ny)
    var mn = Double.MAX_VALUE
    var mx = -Double.MAX_VALUE
    for (i in 0 until count) {
        val v = data[i]
        if (v < mn) mn = v
        if (v > mx) mx = v
    }
    val range = if (mx > mn) mx - mn else 1.0
    val img = BufferedImage(nx, ny, BufferedImage.TYPE_INT_RGB)
    var idx = 0
    for (y in 0 until ny) {
        for (x in 0 until nx) {
            if (idx >= count) break
            val g = (((data[idx++] - mn) / range) * 255.0).toInt().coerceIn(0, 255)
            img.setRGB(x, y, (g shl 16) or (g shl 8) or g)
        }
    }
    return img
}

private fun toDoubleArray(flat: Any?): DoubleArray? = when (flat) {
    is DoubleArray -> flat
    is FloatArray -> DoubleArray(flat.size) { flat[it].toDouble() }
    is IntArray -> DoubleArray(flat.size) { flat[it].toDouble() }
    is ShortArray -> DoubleArray(flat.size) { flat[it].toDouble() }
    is LongArray -> DoubleArray(flat.size) { flat[it].toDouble() }
    is ByteArray -> DoubleArray(flat.size) { (flat[it].toInt() and 0xFF).toDouble() }
    else -> null
}

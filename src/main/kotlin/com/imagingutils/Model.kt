package com.imagingutils

import java.io.File

/** Broad classification of a file so the UI can pick the right reader/renderer. */
enum class ImageKind { PHOTO, RAW, FITS, XISF, OTHER }

data class ImageEntry(
    val file: File,
    val kind: ImageKind,
)

val PHOTO_EXT = setOf("jpg", "jpeg", "png", "tif", "tiff", "bmp", "gif", "webp")
val RAW_EXT = setOf("cr2", "cr3", "nef", "arw", "dng", "raf", "orf", "rw2", "pef", "srw")
val FITS_EXT = setOf("fits", "fit", "fts")
val XISF_EXT = setOf("xisf")

fun classify(file: File): ImageKind = when (file.extension.lowercase()) {
    in PHOTO_EXT -> ImageKind.PHOTO
    in RAW_EXT -> ImageKind.RAW
    in FITS_EXT -> ImageKind.FITS
    in XISF_EXT -> ImageKind.XISF
    else -> ImageKind.OTHER
}

/** Lists supported image files in [dir], sorted by name. Non-recursive for v1. */
fun scanFolder(dir: File): List<ImageEntry> =
    dir.listFiles()
        ?.asSequence()
        ?.filter { it.isFile }
        ?.map { ImageEntry(it, classify(it)) }
        ?.filter { it.kind != ImageKind.OTHER }
        ?.sortedBy { it.file.name.lowercase() }
        ?.toList()
        ?: emptyList()

fun humanSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    return String.format("%.2f GB", mb / 1024.0)
}

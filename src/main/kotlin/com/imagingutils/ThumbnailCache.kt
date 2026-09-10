package com.imagingutils

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bounded, cached thumbnail loader shared by every grid cell.
 *
 * Two problems it solves for large/wide-window folders:
 *  - **Concurrency:** decoding runs on [decodeDispatcher], a view over [Dispatchers.IO]
 *    limited to roughly the CPU count, so widening the window (more visible cells)
 *    queues decodes instead of saturating CPU/IO/memory.
 *  - **Repeat work:** results are held in a small access-ordered LRU [cache], so
 *    scrolling back or re-sorting/filtering returns instantly instead of re-decoding.
 *
 * The cache key includes size + last-modified time, so a file edited on disk is
 * re-decoded rather than served stale.
 */
object ThumbnailCache {

    /** Uniquely identifies a decoded thumbnail; mtime/size invalidate edited files. */
    private data class Key(
        val path: String,
        val size: Long,
        val modified: Long,
        val maxSize: Int,
    )

    /** Decode parallelism: bounded to the CPU count so a wide window can't saturate. */
    private val parallelism: Int = Runtime.getRuntime().availableProcessors().coerceIn(2, 8)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val decodeDispatcher = Dispatchers.IO.limitedParallelism(parallelism)

    /** Roughly 256 thumbnails at 256px ≈ 67 MB; bounds memory while keeping hits high. */
    private const val MAX_ENTRIES = 256

    private val cache = object : LinkedHashMap<Key, ImageBitmap>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<Key, ImageBitmap>): Boolean =
            size > MAX_ENTRIES
    }

    /**
     * Returns a decoded thumbnail for [entry], from cache when available or decoded
     * on the bounded dispatcher otherwise. Returns null for formats that can't be
     * decoded yet (RAW/XISF), matching [loadThumbnail].
     */
    suspend fun get(entry: ImageEntry, maxSize: Int = 256): ImageBitmap? {
        val file = entry.file
        val key = Key(file.absolutePath, file.length(), file.lastModified(), maxSize)
        synchronized(cache) { cache[key] }?.let { return it }
        val bitmap = withContext(decodeDispatcher) { loadThumbnail(entry, maxSize) } ?: return null
        synchronized(cache) { cache[key] = bitmap }
        return bitmap
    }
}

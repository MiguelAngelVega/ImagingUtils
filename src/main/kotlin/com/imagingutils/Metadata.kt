package com.imagingutils

import com.drew.imaging.ImageMetadataReader
import nom.tam.fits.Fits
import nom.tam.fits.Header
import java.io.File

data class MetaRow(val key: String, val value: String)
data class MetaSection(val title: String, val rows: List<MetaRow>)

/** Reads a human-readable view of a file's metadata, grouped into sections. */
fun readMetadata(entry: ImageEntry): List<MetaSection> = buildList {
    add(fileSection(entry.file))
    when (entry.kind) {
        ImageKind.FITS -> addAll(readFits(entry.file))
        else -> addAll(readExif(entry.file))
    }
}

private fun fileSection(file: File): MetaSection = MetaSection(
    "File",
    listOf(
        MetaRow("Name", file.name),
        MetaRow("Folder", file.parent ?: ""),
        MetaRow("Size", humanSize(file.length())),
    ),
)

private fun readExif(file: File): List<MetaSection> = try {
    ImageMetadataReader.readMetadata(file).directories.map { dir ->
        MetaSection(dir.name, dir.tags.map { MetaRow(it.tagName, it.description ?: "") })
    }
} catch (e: Exception) {
    listOf(MetaSection("Metadata", listOf(MetaRow("Unavailable", e.message ?: "n/a"))))
}

private fun readFits(file: File): List<MetaSection> = try {
    Fits(file).use { fits ->
        fits.read().mapIndexed { i, hdu ->
            val header: Header = hdu.header
            val rows = mutableListOf<MetaRow>()
            val cursor = header.iterator()
            while (cursor.hasNext()) {
                val card = cursor.next()
                val key = card.key ?: continue
                if (key.isBlank()) continue
                val comment = card.comment?.takeIf { it.isNotBlank() }?.let { "  / $it" } ?: ""
                rows += MetaRow(key, (card.value ?: "") + comment)
            }
            MetaSection("HDU $i header", rows)
        }
    }
} catch (e: Exception) {
    listOf(MetaSection("FITS", listOf(MetaRow("Unavailable", e.message ?: "n/a"))))
}

package com.imagingutils.data

import com.drew.imaging.ImageMetadataReader
import nom.tam.fits.Fits
import nom.tam.fits.Header
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class MetaRow(val key: String, val value: String, val type: String = "")
data class MetaSection(val title: String, val rows: List<MetaRow>)

private val MODIFIED_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

/** Human-friendly name for a value's Java type (e.g. "String", "int[]"). */
private fun friendlyType(clazz: Class<*>?): String = when {
    clazz == null -> "—"
    clazz.isArray -> friendlyType(clazz.componentType) + "[]"
    else -> clazz.simpleName
}

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
        MetaRow("Name", file.name, "String"),
        MetaRow("Folder", file.parent ?: "", "String"),
        MetaRow("Size", humanSize(file.length()), "String"),
        MetaRow("Modified", MODIFIED_FORMAT.format(Instant.ofEpochMilli(file.lastModified())), "String"),
    ),
)

private fun readExif(file: File): List<MetaSection> = try {
    ImageMetadataReader.readMetadata(file).directories.map { dir ->
        MetaSection(dir.name, dir.tags.map { tag ->
            val raw = dir.getObject(tag.tagType)
            MetaRow(tag.tagName, tag.description ?: "", friendlyType(raw?.javaClass))
        })
    }
} catch (e: Exception) {
    listOf(MetaSection("Metadata", listOf(MetaRow("Unavailable", e.message ?: "n/a", "—"))))
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
                rows += MetaRow(key, (card.value ?: "") + comment, friendlyType(card.valueType()))
            }
            val extName = header.getStringValue("EXTNAME")?.takeIf { it.isNotBlank() }
            val label = when {
                extName != null -> extName
                i == 0 -> "Primary"
                else -> "Extension $i"
            }
            MetaSection("FITS — $label (HDU $i)", rows)
        }
    }
} catch (e: Exception) {
    listOf(MetaSection("FITS", listOf(MetaRow("Unavailable", e.message ?: "n/a", "—"))))
}

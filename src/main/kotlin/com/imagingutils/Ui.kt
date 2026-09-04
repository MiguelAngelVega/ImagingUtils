package com.imagingutils

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.foundation.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FilterBar(
    available: List<String>,
    selected: Set<String>,
    visibleCount: Int,
    onToggle: (String) -> Unit,
) {
    if (available.isEmpty()) return
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "$visibleCount ${if (visibleCount == 1) "file" else "files"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("·", style = MaterialTheme.typography.bodyMedium)
        Text("Filter:", style = MaterialTheme.typography.bodyMedium)
        available.forEach { ext ->
            ExtensionChip(ext, ext in selected) { onToggle(ext) }
        }
    }
}

@Composable
private fun ExtensionChip(ext: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        ".${ext.uppercase()}",
        style = MaterialTheme.typography.labelMedium,
        color = textColor,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
fun ThumbnailGrid(
    entries: List<ImageEntry>,
    selected: ImageEntry?,
    onSelect: (ImageEntry) -> Unit,
    onAutostretch: (ImageEntry) -> Unit,
) {
    if (entries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Open a folder to view images", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    val gridState = rememberLazyGridState()
    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(entries) { entry ->
                ThumbnailCell(entry, entry == selected, onAutostretch) { onSelect(entry) }
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(gridState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
        )
    }
}

@Composable
private fun ThumbnailCell(
    entry: ImageEntry,
    isSelected: Boolean,
    onAutostretch: (ImageEntry) -> Unit,
    onClick: () -> Unit,
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant
    ContextMenuArea(items = { listOf(ContextMenuItem("Autostretch") { onAutostretch(entry) }) }) {
    Column(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            var bitmap by remember(entry) { mutableStateOf<ImageBitmap?>(null) }
            var loading by remember(entry) { mutableStateOf(true) }
            LaunchedEffect(entry) {
                bitmap = withContext(Dispatchers.IO) { loadThumbnail(entry) }
                loading = false
            }
            when {
                bitmap != null -> Image(
                    painter = BitmapPainter(bitmap!!),
                    contentDescription = entry.file.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
                loading -> CircularProgressIndicator(Modifier.height(24.dp))
                else -> Text(entry.kind.name, style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            entry.file.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    }
}

@Composable
fun AutostretchDialog(entry: ImageEntry, onClose: () -> Unit) {
    DialogWindow(
        onCloseRequest = onClose,
        state = rememberDialogState(width = 960.dp, height = 720.dp),
        title = "Autostretch — ${entry.file.name}",
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            var stretched by remember(entry) { mutableStateOf<ImageBitmap?>(null) }
            var original by remember(entry) { mutableStateOf<ImageBitmap?>(null) }
            var loading by remember(entry) { mutableStateOf(true) }
            var showOriginal by remember(entry) { mutableStateOf(false) }
            LaunchedEffect(entry) {
                loading = true
                stretched = withContext(Dispatchers.IO) { loadAutostretchPreview(entry) }
                original = withContext(Dispatchers.IO) { loadThumbnail(entry, 1600) }
                loading = false
            }
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxSize().padding(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            if (showOriginal) "Original (linear)" else "Autostretched",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(
                            onClick = { showOriginal = !showOriginal },
                            enabled = stretched != null && original != null,
                        ) {
                            Text(if (showOriginal) "Show autostretched" else "Compare original")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        val shown = if (showOriginal) original else stretched
                        when {
                            loading -> CircularProgressIndicator()
                            shown != null -> Image(
                                painter = BitmapPainter(shown),
                                contentDescription = entry.file.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                            )
                            else -> Text(
                                "Cannot decode this format for autostretch (${entry.kind.name}).",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataPanel(selected: ImageEntry?) {
    if (selected == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select an image", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    var sections by remember(selected) { mutableStateOf<List<MetaSection>>(emptyList()) }
    var filter by remember(selected) { mutableStateOf("") }
    LaunchedEffect(selected) {
        sections = withContext(Dispatchers.IO) { readMetadata(selected) }
    }
    val query = filter.trim()
    val filtered = if (query.isEmpty()) sections else sections.mapNotNull { section ->
        if (section.title == "File") return@mapNotNull section
        val rows = section.rows.filter {
            it.key.contains(query, ignoreCase = true) ||
                it.value.contains(query, ignoreCase = true) ||
                it.type.contains(query, ignoreCase = true)
        }
        if (rows.isEmpty()) null else section.copy(rows = rows)
    }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = filter,
            onValueChange = { filter = it },
            label = { Text("Filter headers") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            for (section in filtered) {
                Text(section.title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                for (row in section.rows) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                        Text(
                            row.key,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.fillMaxWidth(0.38f),
                        )
                        Text(
                            row.type,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(0.30f),
                        )
                        Text(row.value, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

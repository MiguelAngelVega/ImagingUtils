package com.imagingutils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.foundation.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ThumbnailGrid(
    entries: List<ImageEntry>,
    selected: ImageEntry?,
    onSelect: (ImageEntry) -> Unit,
) {
    if (entries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Open a folder to view images", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(entries, key = { it.file.absolutePath }) { entry ->
            ThumbnailCell(entry, entry == selected) { onSelect(entry) }
        }
    }
}

@Composable
private fun ThumbnailCell(entry: ImageEntry, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant
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

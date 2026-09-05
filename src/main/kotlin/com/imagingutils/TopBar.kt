package com.imagingutils

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun TopBar(
    folder: File?,
    sortKey: SortKey,
    ascending: Boolean,
    onOpen: () -> Unit,
    onSortKey: (SortKey) -> Unit,
    onToggleDirection: () -> Unit,
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(onClick = onOpen) { Text("Open Folder") }
        Spacer(Modifier.width(12.dp))
        Text(
            folder?.absolutePath ?: "No folder selected",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text("Sort:", style = MaterialTheme.typography.bodyMedium)
        SortOption("Name", sortKey == SortKey.NAME) { onSortKey(SortKey.NAME) }
        SortOption("Date", sortKey == SortKey.DATE) { onSortKey(SortKey.DATE) }
        TextButton(onClick = onToggleDirection) {
            Text(if (ascending) "↑ Asc" else "↓ Desc")
        }
        Spacer(Modifier.width(12.dp))
        TextButton(onClick = onToggleTheme) {
            Text(themeMode.label)
        }
    }
}

@Composable
private fun SortOption(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            label,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

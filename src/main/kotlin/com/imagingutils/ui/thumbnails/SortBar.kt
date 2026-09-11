package com.imagingutils.ui.thumbnails

import com.imagingutils.data.SortKey
import com.imagingutils.designsystem.CompactTextButton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Sort controls (key + direction) for the thumbnails panel. Lives at the top of
 * the left column so sorting reads as scoped to the grid it reorders, rather than
 * spanning the whole window from the [TopBar].
 */
@Composable
fun SortBar(
    sortKey: SortKey,
    ascending: Boolean,
    onSortKey: (SortKey) -> Unit,
    onToggleDirection: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("Sort:", style = MaterialTheme.typography.bodyMedium)
        SortOption("Name", sortKey == SortKey.NAME) { onSortKey(SortKey.NAME) }
        SortOption("Date", sortKey == SortKey.DATE) { onSortKey(SortKey.DATE) }
        CompactTextButton(onClick = onToggleDirection) {
            Text(if (ascending) "↑ Asc" else "↓ Desc")
        }
    }
}

@Composable
private fun SortOption(label: String, selected: Boolean, onClick: () -> Unit) {
    CompactTextButton(onClick = onClick) {
        Text(
            label,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

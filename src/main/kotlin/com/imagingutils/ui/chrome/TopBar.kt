package com.imagingutils.ui.chrome

import com.imagingutils.designsystem.CompactButton
import com.imagingutils.designsystem.CompactTextButton
import com.imagingutils.designsystem.ThemeMode

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun TopBar(
    folder: File?,
    onOpen: () -> Unit,
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompactButton(onClick = onOpen) { Text("Open Folder") }
        Spacer(Modifier.width(12.dp))
        Text(
            folder?.absolutePath ?: "No folder selected",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        CompactTextButton(onClick = onToggleTheme) {
            Text(themeMode.label)
        }
    }
}

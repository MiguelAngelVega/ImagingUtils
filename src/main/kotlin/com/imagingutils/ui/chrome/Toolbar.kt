package com.imagingutils.ui.chrome

import com.imagingutils.data.ImageEntry
import com.imagingutils.designsystem.CompactButton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Toolbar(
    selected: ImageEntry?,
    onAutostretch: (ImageEntry) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CompactButton(
            onClick = { selected?.let(onAutostretch) },
            enabled = selected != null,
        ) {
            Text("✨ Autostretch")
        }
    }
}

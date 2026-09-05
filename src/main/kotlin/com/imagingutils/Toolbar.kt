package com.imagingutils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
        /*IconButton(
            onClick = { selected?.let(onAutostretch) },
            enabled = selected != null,
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = "Autostretch")
        }*/
        Button(
            onClick = { selected?.let(onAutostretch) },
            enabled = selected != null,
        ) {
            Text("✨ Autostretch")
        }
    }
}

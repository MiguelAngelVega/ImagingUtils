package com.imagingutils

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "ImagingUtils") {
        MaterialTheme(colorScheme = darkColorScheme()) {
            App()
        }
    }
}

@Composable
private fun App() {
    val scope = rememberCoroutineScope()
    var folder by remember { mutableStateOf<File?>(null) }
    var entries by remember { mutableStateOf<List<ImageEntry>>(emptyList()) }
    var selected by remember { mutableStateOf<ImageEntry?>(null) }

    Column(Modifier.fillMaxSize()) {
        TopBar(folder) {
            val dir = chooseFolder() ?: return@TopBar
            folder = dir
            selected = null
            entries = emptyList()
            scope.launch {
                entries = withContext(Dispatchers.IO) { scanFolder(dir) }
            }
        }
        HorizontalDivider()
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                ThumbnailGrid(entries, selected) { selected = it }
            }
            VerticalDivider()
            Column(Modifier.width(340.dp).fillMaxHeight()) {
                MetadataPanel(selected)
            }
        }
    }
}

@Composable
private fun TopBar(folder: File?, onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(onClick = onOpen) { Text("Open Folder") }
        Spacer(Modifier.width(12.dp))
        Text(
            folder?.absolutePath ?: "No folder selected",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun chooseFolder(): File? {
    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        dialogTitle = "Select image folder"
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}

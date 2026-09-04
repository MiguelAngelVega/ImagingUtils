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
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser

/** How the app picks its color scheme: follow the OS, or force light/dark. */
enum class ThemeMode(val label: String) {
    SYSTEM("🖥 System"),
    LIGHT("☀ Light"),
    DARK("🌙 Dark");

    fun next(): ThemeMode = entries[(ordinal + 1) % entries.size]
}

fun main() = application {
    var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    Window(onCloseRequest = ::exitApplication, title = "ImagingUtils") {
        MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
            App(themeMode = themeMode, onThemeModeChange = { themeMode = it })
        }
    }
}

@Composable
private fun App(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    val scope = rememberCoroutineScope()
    var folder by remember { mutableStateOf<File?>(null) }
    var entries by remember { mutableStateOf<List<ImageEntry>>(emptyList()) }
    var selected by remember { mutableStateOf<ImageEntry?>(null) }
    var sortKey by remember { mutableStateOf(SortKey.NAME) }
    var ascending by remember { mutableStateOf(true) }
    var selectedExtensions by remember { mutableStateOf<Set<String>>(emptySet()) }
    var autostretchTarget by remember { mutableStateOf<ImageEntry?>(null) }
    val availableExts = remember(entries) { availableExtensions(entries) }
    val visibleEntries = remember(entries, selectedExtensions, sortKey, ascending) {
        sortEntries(filterByExtensions(entries, selectedExtensions), sortKey, ascending)
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(Modifier.fillMaxSize()) {
        TopBar(
            folder = folder,
            sortKey = sortKey,
            ascending = ascending,
            onOpen = {
                val dir = chooseFolder()
                if (dir != null) {
                    folder = dir
                    selected = null
                    entries = emptyList()
                    scope.launch {
                        val scanned = withContext(Dispatchers.IO) { scanFolder(dir) }
                        entries = scanned
                        selectedExtensions = availableExtensions(scanned).toSet()
                    }
                }
            },
            onSortKey = { sortKey = it },
            onToggleDirection = { ascending = !ascending },
            themeMode = themeMode,
            onToggleTheme = { onThemeModeChange(themeMode.next()) },
        )
        HorizontalDivider()
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                FilterBar(availableExts, selectedExtensions, visibleEntries.size) { ext ->
                    selectedExtensions = if (ext in selectedExtensions) {
                        selectedExtensions - ext
                    } else {
                        selectedExtensions + ext
                    }
                }
                ThumbnailGrid(
                    visibleEntries,
                    selected,
                    onSelect = { selected = it },
                    onAutostretch = { autostretchTarget = it },
                )
            }
            VerticalDivider()
            Column(Modifier.width(340.dp).fillMaxHeight()) {
                MetadataPanel(selected)
            }
        }
    }
    }
    autostretchTarget?.let { target ->
        AutostretchDialog(target) { autostretchTarget = null }
    }
}

@Composable
private fun TopBar(
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

private fun chooseFolder(): File? {
    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        dialogTitle = "Select image folder"
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}

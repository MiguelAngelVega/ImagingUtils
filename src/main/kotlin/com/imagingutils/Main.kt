package com.imagingutils

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.imagingutils.data.*
import com.imagingutils.designsystem.AppDimens
import com.imagingutils.designsystem.AppTheme
import com.imagingutils.designsystem.ThemeMode
import com.imagingutils.ui.chrome.Toolbar
import com.imagingutils.ui.chrome.TopBar
import com.imagingutils.ui.metadata.MetadataPanel
import com.imagingutils.ui.preview.AutostretchDialog
import com.imagingutils.ui.preview.PreviewPane
import com.imagingutils.ui.thumbnails.FilterBar
import com.imagingutils.ui.thumbnails.SortBar
import com.imagingutils.ui.thumbnails.ThumbnailGrid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Cursor
import java.io.File
import javax.swing.JFileChooser

fun main() = application {
    var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
    Window(onCloseRequest = ::exitApplication, title = "ImagingUtils") {
        AppTheme(themeMode) {
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
    var previewAutostretch by remember { mutableStateOf(false) }
    var thumbnailsWidth by remember { mutableStateOf(AppDimens.thumbnailsPanelDefaultWidth) }
    val availableExts = remember(entries) { availableExtensions(entries) }
    val visibleEntries = remember(entries, selectedExtensions, sortKey, ascending) {
        sortEntries(filterByExtensions(entries, selectedExtensions), sortKey, ascending)
    }

    fun loadFolder(dir: File) {
        folder = dir
        selected = null
        entries = emptyList()
        scope.launch {
            val scanned = withContext(Dispatchers.IO) { scanFolder(dir) }
            entries = scanned
            selectedExtensions = availableExtensions(scanned).toSet()
        }
    }

    LaunchedEffect(Unit) {
        loadLastFolder()?.let { loadFolder(it) }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                folder = folder,
                onOpen = {
                    val dir = chooseFolder(folder ?: loadLastFolder())
                    if (dir != null) {
                        saveLastFolder(dir)
                        loadFolder(dir)
                    }
                },
                themeMode = themeMode,
                onToggleTheme = { onThemeModeChange(themeMode.next()) },
            )
            HorizontalDivider()
            Toolbar(
                selected = selected,
                onAutostretch = { autostretchTarget = it },
            )
            HorizontalDivider()
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val minThumb = AppDimens.thumbnailsPanelMinWidth
                val maxThumb = (maxWidth - AppDimens.metadataPanelWidth - AppDimens.previewPaneMinWidth)
                    .coerceAtLeast(minThumb)
                val thumbWidth = thumbnailsWidth.coerceIn(minThumb, maxThumb)
                Row(Modifier.fillMaxSize()) {
                    Column(Modifier.width(thumbWidth).fillMaxHeight()) {
                        SortBar(
                            sortKey = sortKey,
                            ascending = ascending,
                            onSortKey = { sortKey = it },
                            onToggleDirection = { ascending = !ascending },
                        )
                        HorizontalDivider()
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
                        )
                    }
                    DraggableVerticalDivider { delta ->
                        thumbnailsWidth = (thumbWidth + delta).coerceIn(minThumb, maxThumb)
                    }
                    PreviewPane(
                        entry = selected,
                        autostretch = previewAutostretch,
                        onToggleAutostretch = { previewAutostretch = !previewAutostretch },
                        onEnlarge = { selected?.let { autostretchTarget = it } },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    VerticalDivider()
                    Column(Modifier.width(AppDimens.metadataPanelWidth).fillMaxHeight()) {
                        MetadataPanel(selected)
                    }
                }
            }
        }
    }
    autostretchTarget?.let { target ->
        AutostretchDialog(target) { autostretchTarget = null }
    }
}

/**
 * A [VerticalDivider] with a wider, draggable hit area and an E–W resize cursor.
 * Reports the horizontal drag [delta] in dp so the caller can resize a panel.
 */
@Composable
private fun DraggableVerticalDivider(onDrag: (Dp) -> Unit) {
    val density = LocalDensity.current
    Box(
        Modifier
            .fillMaxHeight()
            .width(AppDimens.spaceSm)
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta -> onDrag(with(density) { delta.toDp() }) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        VerticalDivider()
    }
}

private fun chooseFolder(initialDir: File?): File? {
    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        dialogTitle = "Select image folder"
        initialDir?.takeIf { it.isDirectory }?.let { currentDirectory = it }
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}

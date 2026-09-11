package com.imagingutils.ui.preview

import com.imagingutils.data.ImageEntry
import com.imagingutils.data.loadAutostretchPreview
import com.imagingutils.data.loadThumbnail
import com.imagingutils.designsystem.AppDimens
import com.imagingutils.designsystem.CompactTextButton

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

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
            var scale by remember(entry) { mutableStateOf(1f) }
            var offset by remember(entry) { mutableStateOf(Offset.Zero) }
            var panEnabled by remember(entry) { mutableStateOf(true) }
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
                        Text(
                            "${(scale * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        IconButton(
                            onClick = { scale = (scale / 1.25f).coerceAtLeast(0.1f) },
                            modifier = Modifier.size(AppDimens.iconButtonSize),
                        ) {
                            Icon(
                                Icons.Filled.ZoomOut,
                                contentDescription = "Zoom out",
                                modifier = Modifier.size(AppDimens.iconSize),
                            )
                        }
                        IconButton(
                            onClick = { scale = (scale * 1.25f).coerceAtMost(12f) },
                            modifier = Modifier.size(AppDimens.iconButtonSize),
                        ) {
                            Icon(
                                Icons.Filled.ZoomIn,
                                contentDescription = "Zoom in",
                                modifier = Modifier.size(AppDimens.iconSize),
                            )
                        }
                        IconButton(
                            onClick = { scale = 1f; offset = Offset.Zero },
                            modifier = Modifier.size(AppDimens.iconButtonSize),
                        ) {
                            Icon(
                                Icons.Filled.FitScreen,
                                contentDescription = "Reset zoom",
                                modifier = Modifier.size(AppDimens.iconSize),
                            )
                        }
                        IconToggleButton(
                            checked = panEnabled,
                            onCheckedChange = { panEnabled = it },
                            modifier = Modifier.size(AppDimens.iconButtonSize),
                        ) {
                            Icon(
                                Icons.Filled.PanTool,
                                contentDescription = "Pan",
                                modifier = Modifier.size(AppDimens.iconSize),
                            )
                        }
                        CompactTextButton(
                            onClick = { showOriginal = !showOriginal },
                            enabled = stretched != null && original != null,
                        ) {
                            Text(if (showOriginal) "Show autostretched" else "Compare original")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .pointerInput(entry) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.type == PointerEventType.Scroll) {
                                            val dy = event.changes.first().scrollDelta.y
                                            if (dy != 0f) {
                                                val factor = if (dy < 0) 1.1f else 1f / 1.1f
                                                scale = (scale * factor).coerceIn(0.1f, 12f)
                                            }
                                        }
                                    }
                                }
                            }
                            .then(
                                if (panEnabled) Modifier.pointerInput(entry) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        offset += dragAmount
                                    }
                                } else Modifier,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        val shown = if (showOriginal) original else stretched
                        when {
                            loading -> CircularProgressIndicator()
                            shown != null -> Image(
                                painter = BitmapPainter(shown),
                                contentDescription = entry.file.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y,
                                    ),
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

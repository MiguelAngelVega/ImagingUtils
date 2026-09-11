package com.imagingutils.ui.preview

import com.imagingutils.data.ImageEntry
import com.imagingutils.data.loadAutostretchPreview
import com.imagingutils.data.loadThumbnail
import com.imagingutils.designsystem.AppDimens
import com.imagingutils.designsystem.CompactButton

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Larger than a grid thumbnail so the inline preview shows real detail. */
private const val PREVIEW_MAX = 1024

/**
 * Inline preview of the selected image, shown under the metadata panel. The
 * [autostretch] toggle re-decodes the image with the MTF/STF stretch applied in
 * place, and [onEnlarge] opens the full pan/zoom dialog for the same file.
 */
@Composable
fun PreviewPane(
    entry: ImageEntry?,
    autostretch: Boolean,
    onToggleAutostretch: () -> Unit,
    onEnlarge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(12.dp)) {
        var scale by remember(entry) { mutableStateOf(1f) }
        var offset by remember(entry) { mutableStateOf(Offset.Zero) }
        var panEnabled by remember(entry) { mutableStateOf(true) }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Preview", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            Text("${(scale * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
            IconButton(
                onClick = { scale = (scale / 1.25f).coerceAtLeast(0.1f) },
                enabled = entry != null,
                modifier = Modifier.size(AppDimens.iconButtonSize),
            ) {
                Icon(Icons.Filled.ZoomOut, "Zoom out", Modifier.size(AppDimens.iconSize))
            }
            IconButton(
                onClick = { scale = (scale * 1.25f).coerceAtMost(12f) },
                enabled = entry != null,
                modifier = Modifier.size(AppDimens.iconButtonSize),
            ) {
                Icon(Icons.Filled.ZoomIn, "Zoom in", Modifier.size(AppDimens.iconSize))
            }
            IconButton(
                onClick = { scale = 1f; offset = Offset.Zero },
                enabled = entry != null,
                modifier = Modifier.size(AppDimens.iconButtonSize),
            ) {
                Icon(Icons.Filled.FitScreen, "Original size", Modifier.size(AppDimens.iconSize))
            }
            IconToggleButton(
                checked = panEnabled,
                onCheckedChange = { panEnabled = it },
                enabled = entry != null,
                modifier = Modifier.size(AppDimens.iconButtonSize),
            ) {
                Icon(Icons.Filled.PanTool, "Pan", Modifier.size(AppDimens.iconSize))
            }
            CompactButton(onClick = onToggleAutostretch, enabled = entry != null) {
                Text(if (autostretch) "Autostretch: On" else "Autostretch: Off")
            }
            CompactButton(onClick = onEnlarge, enabled = entry != null) {
                Text("Enlarge")
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxSize()
                .clip(RoundedCornerShape(4.dp))
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
            if (entry == null) {
                Text("Select an image", style = MaterialTheme.typography.bodyMedium)
                return@Box
            }
            var bitmap by remember(entry, autostretch) { mutableStateOf<ImageBitmap?>(null) }
            var loading by remember(entry, autostretch) { mutableStateOf(true) }
            LaunchedEffect(entry, autostretch) {
                loading = true
                bitmap = withContext(Dispatchers.IO) {
                    if (autostretch) loadAutostretchPreview(entry, PREVIEW_MAX)
                    else loadThumbnail(entry, PREVIEW_MAX)
                }
                loading = false
            }
            when {
                bitmap != null -> Image(
                    painter = BitmapPainter(bitmap!!),
                    contentDescription = entry.file.name,
                    modifier = Modifier.fillMaxSize().graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                    ),
                    contentScale = ContentScale.Fit,
                )
                loading -> CircularProgressIndicator()
                else -> Text(
                    "Cannot preview this format (${entry.kind.name}).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

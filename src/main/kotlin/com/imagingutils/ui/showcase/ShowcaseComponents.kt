package com.imagingutils.ui.showcase

import com.imagingutils.data.SortKey
import com.imagingutils.designsystem.AppDimens
import com.imagingutils.designsystem.CompactButton
import com.imagingutils.designsystem.CompactTextButton
import com.imagingutils.designsystem.ThemeMode
import com.imagingutils.ui.chrome.TopBar
import com.imagingutils.ui.thumbnails.FilterBar
import com.imagingutils.ui.thumbnails.SortBar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun TypographyShowcase() {
    val t = MaterialTheme.typography
    val styles: List<Pair<String, TextStyle>> = listOf(
        "displaySmall" to t.displaySmall,
        "headlineMedium" to t.headlineMedium,
        "titleLarge" to t.titleLarge,
        "titleMedium" to t.titleMedium,
        "titleSmall" to t.titleSmall,
        "bodyLarge" to t.bodyLarge,
        "bodyMedium" to t.bodyMedium,
        "bodySmall" to t.bodySmall,
        "labelLarge" to t.labelLarge,
        "labelMedium" to t.labelMedium,
        "labelSmall" to t.labelSmall,
    )
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
        styles.forEach { (name, style) ->
            Text("$name — The quick brown fox", style = style)
        }
    }
}

@Composable
fun ComponentGallery() {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
        Label("Buttons")
        Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
            CompactButton(onClick = {}) { Text("Compact") }
            CompactButton(onClick = {}, enabled = false) { Text("Disabled") }
            CompactTextButton(onClick = {}) { Text("Compact Text") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
            Button(onClick = {}) { Text("Filled") }
            OutlinedButton(onClick = {}) { Text("Outlined") }
            TextButton(onClick = {}) { Text("Text") }
        }

        Label("Text field")
        var text by remember { mutableStateOf("") }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Sample field") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Label("Extension chips (FilterBar)")
        FilterBar(
            available = listOf("fits", "jpg", "cr2", "png"),
            selected = setOf("fits", "png"),
            visibleCount = 4,
            onToggle = {},
        )

        Label("TopBar")
        TopBarDemo()

        Label("SortBar")
        SortBarDemo()

        Label("Progress")
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator()
            LinearProgressIndicator(Modifier.weight(1f))
        }

        Label("Dividers")
        Row(
            Modifier.height(24.dp),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Left", style = MaterialTheme.typography.bodyMedium)
            VerticalDivider()
            Text("Right", style = MaterialTheme.typography.bodyMedium)
        }
        HorizontalDivider()

        Label("Surface (tonal)")
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "surfaceVariant container",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(AppDimens.spaceMd),
            )
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** The real [TopBar] wired to throwaway state so it can be previewed here. */
@Composable
private fun TopBarDemo() {
    var mode by remember { mutableStateOf(ThemeMode.SYSTEM) }
    TopBar(
        folder = null,
        onOpen = {},
        themeMode = mode,
        onToggleTheme = { mode = mode.next() },
    )
}

/** The real [SortBar] wired to throwaway state so it can be previewed here. */
@Composable
private fun SortBarDemo() {
    var sortKey by remember { mutableStateOf(SortKey.NAME) }
    var ascending by remember { mutableStateOf(true) }
    SortBar(
        sortKey = sortKey,
        ascending = ascending,
        onSortKey = { sortKey = it },
        onToggleDirection = { ascending = !ascending },
    )
}

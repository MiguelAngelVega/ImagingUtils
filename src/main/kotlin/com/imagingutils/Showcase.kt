package com.imagingutils

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

/**
 * A standalone gallery app for previewing the design system: every Material color
 * role, the type scale, and the app's reusable components, all under the live
 * theme so light/dark can be compared with the toggle in the header.
 *
 * Run it with: mvn exec:java -Dmain.class=com.imagingutils.ShowcaseKt
 */
fun main() = application {
    var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
    Window(onCloseRequest = ::exitApplication, title = "ImagingUtils — Component Showcase") {
        AppTheme(themeMode) {
            ShowcaseScreen(themeMode) { themeMode = themeMode.next() }
        }
    }
}

@Composable
private fun ShowcaseScreen(themeMode: ThemeMode, onToggleTheme: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(AppDimens.spaceMd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Component Showcase",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                CompactTextButton(onClick = onToggleTheme) { Text(themeMode.label) }
            }
            HorizontalDivider()
            val scroll = rememberScrollState()
            Box(Modifier.fillMaxSize()) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(scroll)
                        .padding(AppDimens.spaceMd).padding(end = AppDimens.spaceMd),
                ) {
                    ShowcaseSection("Colors") { ColorPalette() }
                    ShowcaseSection("Typography") { TypographyShowcase() }
                    ShowcaseSection("Components") { ComponentGallery() }
                }
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(scroll),
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                )
            }
        }
    }
}

/** A titled section with a divider, used to group the showcase content. */
@Composable
fun ShowcaseSection(title: String, content: @Composable () -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(AppDimens.spaceXs))
    HorizontalDivider()
    Spacer(Modifier.height(AppDimens.spaceMd))
    content()
    Spacer(Modifier.height(AppDimens.spaceMd * 2))
}

@Composable
private fun ColorPalette() {
    val cs = MaterialTheme.colorScheme
    val roles = listOf(
        "primary" to cs.primary, "onPrimary" to cs.onPrimary,
        "primaryContainer" to cs.primaryContainer, "onPrimaryContainer" to cs.onPrimaryContainer,
        "inversePrimary" to cs.inversePrimary,
        "secondary" to cs.secondary, "onSecondary" to cs.onSecondary,
        "secondaryContainer" to cs.secondaryContainer, "onSecondaryContainer" to cs.onSecondaryContainer,
        "tertiary" to cs.tertiary, "onTertiary" to cs.onTertiary,
        "tertiaryContainer" to cs.tertiaryContainer, "onTertiaryContainer" to cs.onTertiaryContainer,
        "background" to cs.background, "onBackground" to cs.onBackground,
        "surface" to cs.surface, "onSurface" to cs.onSurface,
        "surfaceVariant" to cs.surfaceVariant, "onSurfaceVariant" to cs.onSurfaceVariant,
        "surfaceTint" to cs.surfaceTint,
        "inverseSurface" to cs.inverseSurface, "inverseOnSurface" to cs.inverseOnSurface,
        "error" to cs.error, "onError" to cs.onError,
        "errorContainer" to cs.errorContainer, "onErrorContainer" to cs.onErrorContainer,
        "outline" to cs.outline, "outlineVariant" to cs.outlineVariant, "scrim" to cs.scrim,
    )
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
        roles.chunked(3).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                rowItems.forEach { (name, color) -> ColorSwatch(name, color, Modifier.weight(1f)) }
                repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ColorSwatch(name: String, color: Color, modifier: Modifier) {
    val fg = if (color.luminance() > 0.5f) Color.Black else Color.White
    Box(
        modifier.height(64.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
            .background(color)
            .padding(AppDimens.spaceSm),
    ) {
        Column {
            Text(name, style = MaterialTheme.typography.labelMedium, color = fg)
            Text("#%08X".format(color.toArgb()), style = MaterialTheme.typography.labelSmall, color = fg)
        }
    }
}

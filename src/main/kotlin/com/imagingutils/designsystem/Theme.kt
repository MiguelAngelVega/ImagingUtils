package com.imagingutils.designsystem

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/** How the app picks its color scheme: follow the OS, or force light/dark. */
enum class ThemeMode(val label: String) {
    SYSTEM("🖥 System"),
    LIGHT("☀ Light"),
    DARK("🌙 Dark");

    fun next(): ThemeMode = entries[(ordinal + 1) % entries.size]
}

/** Applies the app's Material theme, resolving [themeMode] to a light/dark scheme. */
@Composable
fun AppTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors) {
        // Compose Desktop's default scrollbar is black-based, so it disappears on
        // dark backgrounds. Derive it from the theme so it stays visible in both.
        CompositionLocalProvider(
            LocalScrollbarStyle provides defaultScrollbarStyle().copy(
                unhoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                hoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            ),
            content = content,
        )
    }
}
